package org.example.app.data.delivery

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.example.app.data.cart.CartRepository
import org.example.app.data.models.CartLine
import org.example.app.data.storage.PreferencesStorage
import kotlin.math.max
import kotlin.random.Random

/**
 * Singleton repository that owns the simulated delivery state machine:
 * - persisting active order
 * - scheduling stage transitions
 * - emitting LiveData updates for UI
 */
object DeliveryRepository {

    private var appContext: Context? = null
    private var storage: PreferencesStorage? = null

    private val handler = Handler(Looper.getMainLooper())
    private var scheduledRunnable: Runnable? = null
    private var countdownRunnable: Runnable? = null

    private val _activeOrder = MutableLiveData<StoredDeliveryOrder?>(null)
    val activeOrder: LiveData<StoredDeliveryOrder?> = _activeOrder

    private val _etaRemainingMs = MutableLiveData<Long?>(null)
    val etaRemainingMs: LiveData<Long?> = _etaRemainingMs

    // PUBLIC_INTERFACE
    fun initialize(context: Context) {
        /** Initialize delivery repo from persistence and rehydrate any schedules. */
        if (appContext == null) appContext = context.applicationContext
        if (storage == null) storage = PreferencesStorage.from(context.applicationContext)

        NotificationHelper.ensureChannel(context.applicationContext)

        val encoded = storage?.loadActiveDeliveryOrderEncoded()
        val restored = DeliveryCodec.decode(encoded)
        _activeOrder.value = restored

        // Re-schedule based on persisted nextTransitionAtMs.
        scheduleNextTransitionIfNeeded()
        startCountdownTicker()
    }

    // PUBLIC_INTERFACE
    fun placeOrderFromCart(context: Context, restaurantName: String): StoredDeliveryOrder? {
        /**
         * Create and persist a new simulated order based on current cart state.
         * Returns the created order, or null if cart is empty.
         */
        initialize(context)

        val lines: List<CartLine> = CartRepository.cartLines.value ?: emptyList()
        if (lines.isEmpty()) return null

        val itemsSummary = summarizeCart(lines)

        val now = System.currentTimeMillis()
        val orderId = "ORD-" + now.toString().takeLast(6)

        val placed = StoredDeliveryOrder(
            id = orderId,
            restaurantName = restaurantName,
            itemsSummary = itemsSummary,
            createdAtMs = now,
            currentStage = DeliveryStage.PLACED,
            stageTimestampsMs = mapOf(DeliveryStage.PLACED to now),
            nextTransitionAtMs = now + nextDelayMs(),
            orderInstructions = (CartRepository.orderInstructions.value ?: "").trim()
        )

        persistAndPublish(placed)
        NotificationHelper.postStageChanged(context.applicationContext, placed.id, placed.restaurantName, placed.currentStage)

        scheduleNextTransitionIfNeeded()
        startCountdownTicker()
        return placed
    }

    // PUBLIC_INTERFACE
    fun cancelActiveOrder() {
        /** Cancel an active order (before Delivered); clears persistence and stops timers. */
        val current = _activeOrder.value ?: return
        if (current.currentStage == DeliveryStage.DELIVERED) return

        stopTimers()
        _activeOrder.value = null
        _etaRemainingMs.value = null
        storage?.saveActiveDeliveryOrderEncoded(null)
    }

    private fun summarizeCart(lines: List<CartLine>): String {
        // Example: "2x Paneer Tikka, 1x Fries"
        return lines.take(4).joinToString(separator = ", ") { "${it.quantity}x ${it.item.name}" }
            .let { base ->
                val remaining = max(0, lines.size - 4)
                if (remaining > 0) "$base +$remaining more" else base
            }
    }

    private fun nextDelayMs(): Long {
        // 20–30 seconds per stage transition.
        return Random.nextLong(20_000L, 30_001L)
    }

    private fun persistAndPublish(order: StoredDeliveryOrder) {
        _activeOrder.value = order
        storage?.saveActiveDeliveryOrderEncoded(DeliveryCodec.encode(order))
        updateRemainingEta()
    }

    private fun updateRemainingEta() {
        val order = _activeOrder.value
        val now = System.currentTimeMillis()
        val remaining = order?.nextTransitionAtMs?.let { max(0L, it - now) }
        _etaRemainingMs.value = remaining
    }

    private fun scheduleNextTransitionIfNeeded() {
        val order = _activeOrder.value ?: run {
            stopScheduledTransition()
            return
        }

        if (order.currentStage == DeliveryStage.DELIVERED) {
            stopScheduledTransition()
            _etaRemainingMs.value = null
            return
        }

        val nextAt = order.nextTransitionAtMs ?: run {
            stopScheduledTransition()
            return
        }

        stopScheduledTransition()

        val delay = max(0L, nextAt - System.currentTimeMillis())
        val runnable = Runnable { advanceStageIfDue() }
        scheduledRunnable = runnable
        handler.postDelayed(runnable, delay)
    }

    private fun advanceStageIfDue() {
        val context = appContext ?: return
        val current = _activeOrder.value ?: return

        if (current.currentStage == DeliveryStage.DELIVERED) {
            persistAndPublish(current.copy(nextTransitionAtMs = null))
            stopScheduledTransition()
            return
        }

        val now = System.currentTimeMillis()
        val dueAt = current.nextTransitionAtMs ?: return
        if (now < dueAt) {
            // If called early, re-schedule.
            scheduleNextTransitionIfNeeded()
            return
        }

        val nextStage = when (current.currentStage) {
            DeliveryStage.PLACED -> DeliveryStage.ACCEPTED
            DeliveryStage.ACCEPTED -> DeliveryStage.PREPARING
            DeliveryStage.PREPARING -> DeliveryStage.OUT_FOR_DELIVERY
            DeliveryStage.OUT_FOR_DELIVERY -> DeliveryStage.DELIVERED
            DeliveryStage.DELIVERED -> DeliveryStage.DELIVERED
        }

        val updatedTimeline = current.stageTimestampsMs.toMutableMap()
        updatedTimeline[nextStage] = now

        val nextAt = if (nextStage == DeliveryStage.DELIVERED) null else now + nextDelayMs()

        val updated = current.copy(
            currentStage = nextStage,
            stageTimestampsMs = updatedTimeline,
            nextTransitionAtMs = nextAt
        )

        persistAndPublish(updated)
        NotificationHelper.postStageChanged(context, updated.id, updated.restaurantName, updated.currentStage)

        scheduleNextTransitionIfNeeded()
    }

    private fun startCountdownTicker() {
        stopCountdownTicker()
        val runnable = object : Runnable {
            override fun run() {
                updateRemainingEta()
                handler.postDelayed(this, 1_000L)
            }
        }
        countdownRunnable = runnable
        handler.post(runnable)
    }

    private fun stopScheduledTransition() {
        scheduledRunnable?.let { handler.removeCallbacks(it) }
        scheduledRunnable = null
    }

    private fun stopCountdownTicker() {
        countdownRunnable?.let { handler.removeCallbacks(it) }
        countdownRunnable = null
    }

    private fun stopTimers() {
        stopScheduledTransition()
        stopCountdownTicker()
    }
}
