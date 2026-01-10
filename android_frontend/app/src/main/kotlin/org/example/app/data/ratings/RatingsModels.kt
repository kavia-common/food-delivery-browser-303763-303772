package org.example.app.data.ratings

/**
 * The target entity a review belongs to.
 */
enum class ReviewTargetType {
    RESTAURANT,
    MENU_ITEM
}

/**
 * Stable identifier for a review target.
 */
data class ReviewTarget(
    val type: ReviewTargetType,
    val id: String
) {
    fun stableKey(): String = "${type.name}:$id"
}

/**
 * User-authored review persisted locally.
 *
 * @param rating Integer in [1..5]
 */
data class Review(
    val id: String,
    val targetType: ReviewTargetType,
    val targetId: String,
    val authorName: String,
    val rating: Int,
    val text: String,
    val createdAtMs: Long,
    val updatedAtMs: Long
) {
    fun target(): ReviewTarget = ReviewTarget(targetType, targetId)
}

/**
 * Draft for creating a review (id/timestamps assigned by repository).
 */
data class ReviewDraft(
    val authorName: String,
    val rating: Int,
    val text: String
)

/**
 * Update payload for editing an existing review.
 */
data class ReviewUpdate(
    val authorName: String? = null,
    val rating: Int? = null,
    val text: String? = null
)

/**
 * Aggregate metadata for quick display.
 */
data class RatingAggregate(
    val average: Double,
    val count: Int
)
