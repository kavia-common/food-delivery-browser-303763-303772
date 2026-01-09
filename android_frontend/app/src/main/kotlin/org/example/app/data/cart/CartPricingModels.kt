package org.example.app.data.cart

/**
 * Models used by [CartRepository] to compute pricing breakdowns and to persist/restore promo settings.
 */

enum class PromoKind {
    PERCENT_OFF,
    FIXED_CENTS_OFF
}

data class AppliedPromo(
    val code: String,
    val kind: PromoKind,
    val value: Int // percent (e.g., 10) or cents (e.g., 500)
)

data class FeeSettings(
    val deliveryFeeCents: Int,
    val serviceFeeCents: Int,
    val taxRate: Double
)

data class CartTotals(
    val itemCount: Int,
    val subtotalCents: Int,
    val discountCents: Int,
    val deliveryFeeCents: Int,
    val serviceFeeCents: Int,
    val taxCents: Int,
    val totalCents: Int
)
