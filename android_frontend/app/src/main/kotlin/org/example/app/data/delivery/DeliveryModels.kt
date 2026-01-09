package org.example.app.data.delivery

/**
 * Delivery stages used by the simulated tracking flow.
 */
enum class DeliveryStage {
    PLACED,
    ACCEPTED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED;

    fun displayLabel(): String {
        return when (this) {
            PLACED -> "Placed"
            ACCEPTED -> "Accepted"
            PREPARING -> "Preparing"
            OUT_FOR_DELIVERY -> "Out for delivery"
            DELIVERED -> "Delivered"
        }
    }
}

/**
 * Persisted model for an active simulated order.
 *
 * Times are epoch millis.
 */
data class StoredDeliveryOrder(
    val id: String,
    val restaurantName: String,
    val itemsSummary: String,
    val createdAtMs: Long,
    val currentStage: DeliveryStage,
    val stageTimestampsMs: Map<DeliveryStage, Long>,
    val nextTransitionAtMs: Long?
)
