package org.example.app.data.storage.models

/**
 * Minimal persisted representation of a cart line.
 * We store enough fields to restore UI and totals without needing a backend.
 *
 * @param configurationKey Stable key used for distinguishing multiple configurations of the same item.
 * @param selectedVariantOptionIds Encoded as "groupId=optionId,groupId=optionId" (escaped by SafeCodec).
 * @param selectedAddOnOptionIds Encoded as "optionId,optionId" (escaped by SafeCodec).
 */
data class StoredCartLine(
    val itemId: String,
    val restaurantId: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val priceCents: Int,
    val isVeg: Boolean,
    val quantity: Int,
    val configurationKey: String = "",
    val selectedVariantOptionIds: String = "",
    val selectedAddOnOptionIds: String = "",
    val itemNote: String = ""
)
