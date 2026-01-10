package org.example.app.data.recommendations

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import org.example.app.data.favorites.FavoritesRepository
import org.example.app.data.mock.MockData
import org.example.app.data.models.MenuItem
import org.example.app.data.models.Restaurant
import org.example.app.data.ratings.RatingsRepository
import org.example.app.data.ratings.ReviewTarget
import org.example.app.data.ratings.ReviewTargetType
import org.example.app.data.recent.RecentlyViewedRepository
import kotlin.math.roundToInt

/**
 * Simple local-only recommendations.
 *
 * Priority (Home - restaurants):
 *  1) Favorites
 *  2) Cuisines most represented in recently viewed restaurants
 *  3) Highly rated (mock rating, then user aggregate if present)
 *
 * Menu (for a restaurant - items):
 *  - items from that restaurant, boosted by favorited items + recently viewed items
 *  - fallback to top priced/popular-ish (mock heuristic) if little signal
 *
 * This is intentionally lightweight and deterministic (no backend).
 */
object RecommendationsRepository {

    private const val HOME_LIMIT = 10
    private const val MENU_LIMIT = 10

    // Cache to avoid heavy recomputation. Invalidated on any input change.
    @Volatile
    private var homeCacheKey: Int = 0

    @Volatile
    private var homeCache: List<Restaurant> = emptyList()

    private val homeRecommendedLive = MediatorLiveData<List<Restaurant>>()

    init {
        // When any relevant signal changes, invalidate and recompute.
        homeRecommendedLive.addSource(FavoritesRepository.favoriteRestaurantIdsLive) { invalidateHome() }
        homeRecommendedLive.addSource(RecentlyViewedRepository.recentRestaurantIdsLive) { invalidateHome() }
        // Ratings are not LiveData-per-restaurant here; we treat ratings as a minor tiebreaker using sync getter.
        homeRecommendedLive.value = computeHomeRecommendations()
    }

    // PUBLIC_INTERFACE
    fun homeRecommendations(): LiveData<List<Restaurant>> {
        /** LiveData for Home "Recommended for you" restaurants. */
        if (homeRecommendedLive.value == null) homeRecommendedLive.value = computeHomeRecommendations()
        return homeRecommendedLive
    }

    // PUBLIC_INTERFACE
    fun recommendationsForRestaurantMenu(restaurantId: String): LiveData<List<MenuItem>> {
        /** LiveData for Menu "You might also like" items for a specific restaurant. */
        val live = MediatorLiveData<List<MenuItem>>()

        fun recompute() {
            live.value = computeMenuRecommendations(restaurantId)
        }

        live.addSource(FavoritesRepository.favoriteMenuItemIdsLive) { recompute() }
        live.addSource(RecentlyViewedRepository.recentMenuItemIdsLive) { recompute() }
        // Also allow restaurant favorites to influence indirectly (small boost).
        live.addSource(FavoritesRepository.favoriteRestaurantIdsLive) { recompute() }

        recompute()
        return live
    }

    private fun invalidateHome() {
        homeCacheKey = 0
        homeCache = emptyList()
        homeRecommendedLive.value = computeHomeRecommendations()
    }

    private fun computeHomeRecommendations(): List<Restaurant> {
        val favIds = FavoritesRepository.favoriteRestaurantIdsLive.value ?: emptySet()
        val recentRestaurantIds = RecentlyViewedRepository.recentRestaurantIdsLive.value ?: emptyList()

        // Build a simple key based on inputs (order matters for recents).
        val key = (favIds.hashCode() * 31) + recentRestaurantIds.hashCode()
        if (key == homeCacheKey && homeCache.isNotEmpty()) return homeCache

        val all = MockData.restaurants

        // Cuisine frequency from recently viewed restaurants.
        val cuisineCounts: Map<String, Int> = recentRestaurantIds
            .mapNotNull { id -> MockData.restaurantById(id) }
            .flatMap { it.cuisineTags }
            .groupingBy { it }
            .eachCount()

        fun score(r: Restaurant): Int {
            val favBoost = if (favIds.contains(r.id)) 10_000 else 0
            val cuisineBoost = r.cuisineTags.sumOf { (cuisineCounts[it] ?: 0) } * 200

            // Base score from mock rating; also add small bump from user rating aggregate if exists.
            val userAgg = RatingsRepository.getAggregateNow(ReviewTarget(ReviewTargetType.RESTAURANT, r.id))
            val userBoost = if (userAgg != null && userAgg.count > 0) (userAgg.average * 100.0).roundToInt() else 0

            val ratingScore = (r.rating * 1000.0).roundToInt()
            return favBoost + cuisineBoost + userBoost + ratingScore
        }

        // De-duplicate while keeping ranking.
        val recommended = all
            .sortedWith(compareByDescending<Restaurant> { score(it) }.thenBy { it.name })
            .take(HOME_LIMIT)
            .distinctBy { it.id }

        homeCacheKey = key
        homeCache = recommended
        return recommended
    }

    private fun computeMenuRecommendations(restaurantId: String): List<MenuItem> {
        val allItems = MockData.menuForRestaurant(restaurantId)
        if (allItems.isEmpty()) return emptyList()

        val favItemIds = FavoritesRepository.favoriteMenuItemIdsLive.value ?: emptySet()
        val recentItemIds = RecentlyViewedRepository.recentMenuItemIdsLive.value ?: emptyList()

        // For the menu, signals are item IDs. Recent views across the app still influence (if the same item ID exists).
        val recentIndex: Map<String, Int> = recentItemIds.withIndex().associate { it.value to it.index }

        fun score(item: MenuItem): Int {
            val favBoost = if (favItemIds.contains(item.id)) 5_000 else 0
            val recentBoost = recentIndex[item.id]?.let { idx -> (MAX_RECENT_WEIGHT - idx).coerceAtLeast(0) * 100 } ?: 0

            // Use a small heuristic for "popular": slightly prefer higher price as proxy for signature items (demo-only).
            val priceBoost = (item.priceCents / 10).coerceAtMost(300)

            // Prefer items with options a bit (more interesting).
            val optionsBoost = if (item.variantGroups.isNotEmpty() || item.addOnGroups.isNotEmpty()) 120 else 0

            return favBoost + recentBoost + priceBoost + optionsBoost
        }

        return allItems
            .sortedWith(compareByDescending<MenuItem> { score(it) }.thenBy { it.name })
            .take(MENU_LIMIT)
            .distinctBy { it.id }
    }

    private const val MAX_RECENT_WEIGHT = 20
}
