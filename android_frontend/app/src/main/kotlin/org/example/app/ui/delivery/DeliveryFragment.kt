package org.example.app.ui.delivery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import org.example.app.ui.common.MotionUtils
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.example.app.R
import org.example.app.data.delivery.DeliveryRepository
import org.example.app.data.delivery.DeliveryStage
import org.example.app.data.delivery.StoredDeliveryOrder
import org.example.app.ui.shared.SharedDeliveryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class DeliveryFragment : Fragment() {

    private lateinit var sharedDeliveryViewModel: SharedDeliveryViewModel

    private lateinit var emptyState: View
    private lateinit var content: View

    private lateinit var headerTitle: TextView
    private lateinit var headerSubtitle: TextView
    private lateinit var headerInstructions: TextView
    private lateinit var headerEta: TextView
    private lateinit var headerRing: ProgressBar
    private lateinit var headerRingPercent: TextView
    private lateinit var headerProgress: ProgressBar
    private lateinit var cancelButton: MaterialButton

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DeliveryTimelineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedDeliveryViewModel = ViewModelProvider(requireActivity())[SharedDeliveryViewModel::class.java]
        adapter = DeliveryTimelineAdapter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_delivery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        emptyState = view.findViewById(R.id.deliveryEmptyState)
        content = view.findViewById(R.id.deliveryContent)

        headerTitle = view.findViewById(R.id.deliveryHeaderTitle)
        headerSubtitle = view.findViewById(R.id.deliveryHeaderSubtitle)
        headerInstructions = view.findViewById(R.id.deliveryOrderInstructions)
        headerEta = view.findViewById(R.id.deliveryHeaderEta)
        headerRing = view.findViewById(R.id.deliveryHeaderRing)
        headerRingPercent = view.findViewById(R.id.deliveryHeaderRingPercent)
        headerProgress = view.findViewById(R.id.deliveryHeaderProgress)
        cancelButton = view.findViewById(R.id.cancelOrderButton)

        recyclerView = view.findViewById(R.id.deliveryTimelineRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        cancelButton.setOnClickListener {
            DeliveryRepository.cancelActiveOrder()
        }

        sharedDeliveryViewModel.activeOrder.observe(viewLifecycleOwner) { order ->
            render(order, sharedDeliveryViewModel.etaRemainingMs.value)
        }
        sharedDeliveryViewModel.etaRemainingMs.observe(viewLifecycleOwner) { remaining ->
            render(sharedDeliveryViewModel.activeOrder.value, remaining)
        }
    }

    private fun render(order: StoredDeliveryOrder?, etaRemainingMs: Long?) {
        val hasOrder = order != null
        emptyState.isVisible = !hasOrder
        content.isVisible = hasOrder

        if (order == null) return

        headerTitle.text = getString(R.string.delivery_tracking_title)
        headerSubtitle.text = getString(R.string.delivery_tracking_subtitle, order.restaurantName, order.itemsSummary)

        val instructions = order.orderInstructions.trim()
        headerInstructions.isVisible = instructions.isNotBlank()
        headerInstructions.text = if (instructions.isNotBlank()) {
            getString(R.string.order_instructions_display, instructions)
        } else {
            ""
        }

        val stageIndex = order.currentStage.ordinal
        val progressPct = ((stageIndex + 1) * 100 / DeliveryStage.entries.size).coerceIn(0, 100)

        headerProgress.progress = progressPct
        headerRing.progress = progressPct
        headerRingPercent.text = "$progressPct%"

        val isDelivered = order.currentStage == DeliveryStage.DELIVERED
        cancelButton.isVisible = !isDelivered

        headerEta.isVisible = !isDelivered
        val etaText = if (isDelivered) "" else {
            val seconds = max(0L, (etaRemainingMs ?: 0L) / 1000L)
            getString(R.string.delivery_eta_in, formatCountdown(seconds))
        }
        headerEta.text = etaText

        // Accessibility: give the ring a stage + ETA summary.
        headerRing.contentDescription = if (isDelivered) {
            "Delivery progress: ${order.currentStage.displayLabel()}, completed."
        } else {
            "Delivery progress: ${order.currentStage.displayLabel()}. $etaText."
        }

        adapter.submit(buildTimelineRows(order, etaRemainingMs))
    }

    private fun buildTimelineRows(order: StoredDeliveryOrder, etaRemainingMs: Long?): List<DeliveryTimelineRow> {
        val now = System.currentTimeMillis()
        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())

        fun formatTime(ms: Long?): String {
            return if (ms == null || ms <= 0L) {
                getString(R.string.delivery_time_pending)
            } else {
                timeFmt.format(Date(ms))
            }
        }

        val current = order.currentStage
        val nextAt = order.nextTransitionAtMs

        return DeliveryStage.entries.map { stage ->
            val completed = stage.ordinal < current.ordinal || stage == DeliveryStage.DELIVERED && current == DeliveryStage.DELIVERED
            val isCurrent = stage == current

            val timestamp = order.stageTimestampsMs[stage]
            val timeText = if (timestamp != null) formatTime(timestamp) else {
                if (stage.ordinal <= current.ordinal) formatTime(now) else getString(R.string.delivery_time_pending)
            }

            val etaText = if (isCurrent && current != DeliveryStage.DELIVERED && nextAt != null) {
                val sec = max(0L, (etaRemainingMs ?: max(0L, nextAt - now)) / 1000L)
                getString(R.string.delivery_next_update_in, formatCountdown(sec))
            } else null

            DeliveryTimelineRow(
                stage = stage,
                isCompleted = completed,
                isCurrent = isCurrent,
                timeText = timeText,
                etaText = etaText
            )
        }
    }

    private fun formatCountdown(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    companion object {
        const val TAG = "DeliveryFragment"

        fun newInstance(): DeliveryFragment = DeliveryFragment()
    }
}
