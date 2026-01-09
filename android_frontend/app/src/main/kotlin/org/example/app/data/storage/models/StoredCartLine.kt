package org.example.app.data.storage.models

/**
 * Minimal persisted representation of a cart line.
 * We store enough fields to restore UI and totals without needing a backend.
 */
data class StoredCartLine(
    val itemId: String,
    val restaurantId: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val priceCents: Int,
    val isVeg: Boolean,
    val quantity: Int
)
