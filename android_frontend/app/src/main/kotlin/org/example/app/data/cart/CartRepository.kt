package org.example.app.data.cart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.example.app.data.models.CartLine
import org.example.app.data.models.MenuItem
import kotlin.math.max

/**
 * In-memory cart repository (singleton).
 *
 * Keeps cart state during app runtime. Configuration changes are handled via ViewModel
 * observing LiveData exposed here.
 */
object CartRepository {

    private val linesByItemId: LinkedHashMap<String, CartLine> = LinkedHashMap()

    private val _cartLines = MutableLiveData<List<CartLine>>(emptyList())
    val cartLines: LiveData<List<CartLine>> = _cartLines

    private val _totalItemCount = MutableLiveData(0)
    val totalItemCount: LiveData<Int> = _totalItemCount

    private const val DELIVERY_FEE_CENTS = 199
    private const val TAX_RATE = 0.08

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

    private fun publish() {
        val list = linesByItemId.values.toList()
        _cartLines.value = list
        _totalItemCount.value = list.sumOf { it.quantity }
    }
}
