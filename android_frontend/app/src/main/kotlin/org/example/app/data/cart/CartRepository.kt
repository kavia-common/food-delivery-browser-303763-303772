package org.example.app.data.cart

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.example.app.data.models.CartLine
import org.example.app.data.models.MenuItem
import org.example.app.data.storage.PreferencesStorage
import org.example.app.data.storage.models.StoredCartLine
import kotlin.math.max

/**
 * In-memory cart repository (singleton) with local persistence.
 *
 * Keeps cart state during app runtime. Configuration changes are handled via ViewModel
 * observing LiveData exposed here.
 *
 * Call initialize(context) once on app start to hydrate from persisted state.
 */
object CartRepository {

    private var storage: PreferencesStorage? = null

    private val linesByItemId: LinkedHashMap<String, CartLine> = LinkedHashMap()

    private val _cartLines = MutableLiveData<List<CartLine>>(emptyList())
    val cartLines: LiveData<List<CartLine>> = _cartLines

    private val _totalItemCount = MutableLiveData(0)
    val totalItemCount: LiveData<Int> = _totalItemCount

    private const val DELIVERY_FEE_CENTS = 199
    private const val TAX_RATE = 0.08

    // PUBLIC_INTERFACE
    fun initialize(context: Context) {
        /** Initialize repo from SharedPreferences. Safe to call multiple times. */
        if (storage == null) storage = PreferencesStorage.from(context)
        val st = storage ?: return

        linesByItemId.clear()
        st.loadCartLines().forEach { stored ->
            val item = MenuItem(
                id = stored.itemId,
                restaurantId = stored.restaurantId,
                categoryId = stored.categoryId,
                name = stored.name,
                description = stored.description,
                priceCents = stored.priceCents,
                isVeg = stored.isVeg
            )
            if (stored.quantity > 0) {
                linesByItemId[item.id] = CartLine(item, stored.quantity)
            }
        }
        publish(persist = false)
    }

    // PUBLIC_INTERFACE
    fun add(item: MenuItem) {
        /** Add one quantity for a menu item. */
        val existing = linesByItemId[item.id]
        val newQ = (existing?.quantity ?: 0) + 1
        linesByItemId[item.id] = CartLine(item, newQ)
        publish()
    }

    // PUBLIC_INTERFACE
    fun remove(item: MenuItem) {
        /** Remove the line item entirely. */
        linesByItemId.remove(item.id)
        publish()
    }

    // PUBLIC_INTERFACE
    fun updateQuantity(item: MenuItem, quantity: Int) {
        /** Set item quantity (<=0 removes). */
        val q = max(0, quantity)
        if (q <= 0) linesByItemId.remove(item.id) else linesByItemId[item.id] = CartLine(item, q)
        publish()
    }

    // PUBLIC_INTERFACE
    fun getQuantity(itemId: String): Int {
        /** Get current quantity for an item ID. */
        return linesByItemId[itemId]?.quantity ?: 0
    }

    // PUBLIC_INTERFACE
    fun computeSubtotalCents(): Int {
        /** Sum of (price * quantity). */
        return linesByItemId.values.sumOf { it.item.priceCents * it.quantity }
    }

    // PUBLIC_INTERFACE
    fun computeDeliveryFeeCents(): Int {
        /** Flat mock delivery fee; 0 if cart empty. */
        return if (linesByItemId.isEmpty()) 0 else DELIVERY_FEE_CENTS
    }

    // PUBLIC_INTERFACE
    fun computeTaxCents(): Int {
        /** Mock tax percent applied on subtotal. */
        return (computeSubtotalCents() * TAX_RATE).toInt()
    }

    // PUBLIC_INTERFACE
    fun computeTotalCents(): Int {
        /** Total = subtotal + delivery + tax. */
        return computeSubtotalCents() + computeDeliveryFeeCents() + computeTaxCents()
    }

    private fun publish(persist: Boolean = true) {
        val list = linesByItemId.values.toList()
        _cartLines.value = list
        _totalItemCount.value = list.sumOf { it.quantity }

        if (persist) persistCart(list)
    }

    private fun persistCart(lines: List<CartLine>) {
        // If initialize() wasn't called yet, skip persistence (no context available).
        val st = storage ?: return
        val storedLines = lines.map { line ->
            StoredCartLine(
                itemId = line.item.id,
                restaurantId = line.item.restaurantId,
                categoryId = line.item.categoryId,
                name = line.item.name,
                description = line.item.description,
                priceCents = line.item.priceCents,
                isVeg = line.item.isVeg,
                quantity = line.quantity
            )
        }
        st.saveCartLines(storedLines)
    }
}
