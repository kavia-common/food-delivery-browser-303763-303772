package org.example.app.data.ratings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.example.app.data.storage.PreferencesStorage
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Local-only ratings & reviews repository.
 *
 * This repository:
 * - Hydrates from SharedPreferences on initialize(context)
 * - Maintains in-memory indexes for fast per-target listing
 * - Maintains per-target aggregates for fast display on Home/Menu
 * - Persists immediately after each mutation
 */
object RatingsRepository {

    private var storage: PreferencesStorage? = null

    // In-memory source of truth.
    private val reviewsById: LinkedHashMap<String, Review> = LinkedHashMap()
    private val reviewsByTarget: LinkedHashMap<ReviewTarget, MutableList<String>> = LinkedHashMap()
    private val aggregatesByTarget: LinkedHashMap<ReviewTarget, RatingAggregate> = LinkedHashMap()

    // LiveData fan-out caches (per target).
    private val reviewsLiveDataByTarget: LinkedHashMap<ReviewTarget, MutableLiveData<List<Review>>> = LinkedHashMap()
    private val aggregateLiveDataByTarget: LinkedHashMap<ReviewTarget, MutableLiveData<RatingAggregate?>> = LinkedHashMap()

    // PUBLIC_INTERFACE
    fun initialize(context: Context) {
        /** Initialize/hydrate repository from local persistence. Safe to call multiple times. */
        if (storage != null) return
        storage = PreferencesStorage.from(context)

        val encodedReviews = storage?.loadRatingsReviewsEncoded()
        val decodedReviews = RatingsCodec.decodeReviews(encodedReviews)

        reviewsById.clear()
        reviewsByTarget.clear()

        decodedReviews.forEach { r ->
            reviewsById[r.id] = r
            val target = r.target()
            val list = reviewsByTarget.getOrPut(target) { mutableListOf() }
            list.add(r.id)
        }

        // Keep stable ordering: newest first in each target list.
        reviewsByTarget.forEach { (_, ids) ->
            ids.sortByDescending { id -> reviewsById[id]?.createdAtMs ?: 0L }
        }

        // Load aggregates if present; otherwise compute (backward compatible if aggregates missing/corrupt).
        val decodedAgg = RatingsCodec.decodeAggregates(storage?.loadRatingsAggregatesEncoded())
        aggregatesByTarget.clear()
        if (decodedAgg.isNotEmpty()) {
            aggregatesByTarget.putAll(decodedAgg)
        } else {
            recomputeAllAggregates()
        }

        // Publish initial values to any existing LiveData observers.
        publishAllTargets()
    }

    // PUBLIC_INTERFACE
    fun getReviews(target: ReviewTarget): LiveData<List<Review>> {
        /** Get a LiveData stream of reviews (newest-first) for a given target. */
        return reviewsLiveDataByTarget.getOrPut(target) {
            MutableLiveData<List<Review>>().also { it.value = currentReviewsForTarget(target) }
        }
    }

    // PUBLIC_INTERFACE
    fun getAggregate(target: ReviewTarget): LiveData<RatingAggregate?> {
        /** Get a LiveData stream of aggregate rating metadata for a target. */
        return aggregateLiveDataByTarget.getOrPut(target) {
            MutableLiveData<RatingAggregate?>().also { it.value = aggregatesByTarget[target] }
        }
    }

    // PUBLIC_INTERFACE
    fun getAggregateNow(target: ReviewTarget): RatingAggregate? {
        /** Synchronous accessor for aggregate (for binding in adapters). */
        return aggregatesByTarget[target]
    }

    // PUBLIC_INTERFACE
    fun addReview(target: ReviewTarget, draft: ReviewDraft): Review? {
        /**
         * Add a new review; returns the created Review or null when validation fails.
         * Validation: authorName non-blank, rating in [1..5], text max length enforced by UI.
         */
        val author = draft.authorName.trim()
        if (author.isBlank()) return null
        if (draft.rating !in 1..5) return null

        val now = System.currentTimeMillis()
        val review = Review(
            id = UUID.randomUUID().toString(),
            targetType = target.type,
            targetId = target.id,
            authorName = author,
            rating = draft.rating,
            text = draft.text,
            createdAtMs = now,
            updatedAtMs = now
        )

        reviewsById[review.id] = review
        val ids = reviewsByTarget.getOrPut(target) { mutableListOf() }
        ids.add(0, review.id) // newest first

        recomputeAggregateForTarget(target)
        persist()
        publishTarget(target)
        return review
    }

    // PUBLIC_INTERFACE
    fun updateReview(reviewId: String, changes: ReviewUpdate): Boolean {
        /**
         * Update an existing review in place. Returns true if updated, false if not found/invalid.
         */
        val existing = reviewsById[reviewId] ?: return false
        val newAuthor = (changes.authorName ?: existing.authorName).trim()
        val newRating = changes.rating ?: existing.rating
        val newText = changes.text ?: existing.text

        if (newAuthor.isBlank()) return false
        if (newRating !in 1..5) return false

        val updated = existing.copy(
            authorName = newAuthor,
            rating = newRating,
            text = newText,
            updatedAtMs = System.currentTimeMillis()
        )
        reviewsById[reviewId] = updated

        val target = updated.target()
        // Ordering by createdAt; no need to re-sort unless target changed (not supported).
        recomputeAggregateForTarget(target)
        persist()
        publishTarget(target)
        return true
    }

    // PUBLIC_INTERFACE
    fun deleteReview(reviewId: String): Boolean {
        /** Delete a review by id; returns true if deleted. */
        val existing = reviewsById.remove(reviewId) ?: return false
        val target = existing.target()

        reviewsByTarget[target]?.remove(reviewId)
        if (reviewsByTarget[target]?.isEmpty() == true) {
            reviewsByTarget.remove(target)
        }

        recomputeAggregateForTarget(target)
        persist()
        publishTarget(target)
        return true
    }

    private fun currentReviewsForTarget(target: ReviewTarget): List<Review> {
        val ids = reviewsByTarget[target] ?: return emptyList()
        return ids.mapNotNull { reviewsById[it] }
            .sortedWith(compareByDescending<Review> { it.createdAtMs }.thenByDescending { it.updatedAtMs })
    }

    private fun recomputeAllAggregates() {
        val targets = reviewsByTarget.keys.toList()
        targets.forEach { recomputeAggregateForTarget(it) }
    }

    private fun recomputeAggregateForTarget(target: ReviewTarget) {
        val reviews = currentReviewsForTarget(target)
        if (reviews.isEmpty()) {
            aggregatesByTarget.remove(target)
            return
        }
        val avg = reviews.map { it.rating }.average()
        // Keep a 1-decimal-ish stable value (UI uses Formatters anyway) but avoid long decimals in persistence.
        val normalizedAvg = (avg * 10.0).roundToInt() / 10.0
        aggregatesByTarget[target] = RatingAggregate(average = normalizedAvg, count = reviews.size)
    }

    private fun persist() {
        val s = storage ?: return
        // Persist reviews and aggregates as separate blobs for compatibility and quick reads.
        val encodedReviews = RatingsCodec.encodeReviews(reviewsById.values.toList())
        val encodedAgg = RatingsCodec.encodeAggregates(aggregatesByTarget.toMap())
        s.saveRatingsReviewsEncoded(encodedReviews)
        s.saveRatingsAggregatesEncoded(encodedAgg)
    }

    private fun publishAllTargets() {
        (reviewsByTarget.keys + reviewsLiveDataByTarget.keys).toSet().forEach { publishTarget(it) }
    }

    private fun publishTarget(target: ReviewTarget) {
        val list = currentReviewsForTarget(target)
        reviewsLiveDataByTarget.getOrPut(target) { MutableLiveData() }.postValue(list)
        aggregateLiveDataByTarget.getOrPut(target) { MutableLiveData() }.postValue(aggregatesByTarget[target])
    }
}
