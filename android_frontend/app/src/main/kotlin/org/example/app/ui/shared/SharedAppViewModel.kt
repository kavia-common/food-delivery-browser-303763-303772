package org.example.app.ui.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.example.app.data.favorites.FavoritesRepository
import org.example.app.data.models.MenuItem
import org.example.app.data.models.Restaurant
import org.example.app.data.preferences.AppPreferencesRepository
import org.example.app.data.recommendations.RecommendationsRepository

class SharedAppViewModel : ViewModel() {
    val favoriteRestaurantIds: LiveData<Set<String>> = FavoritesRepository.favoriteRestaurantIdsLive
    val favoriteMenuItemIds: LiveData<Set<String>> = FavoritesRepository.favoriteMenuItemIdsLive

    val themeMode: LiveData<AppPreferencesRepository.ThemeMode> = AppPreferencesRepository.themeMode

    val homeFavoritesOnly: LiveData<Boolean> = AppPreferencesRepository.homeFavoritesOnly
    val homeSearchQuery: LiveData<String> = AppPreferencesRepository.homeSearchQuery
    val homeVegOnly: LiveData<Boolean> = AppPreferencesRepository.homeVegOnly
    val homeSelectedCuisines: LiveData<Set<String>> = AppPreferencesRepository.homeSelectedCuisines
    val homeSortOption: LiveData<AppPreferencesRepository.HomeSortOption> = AppPreferencesRepository.homeSortOption

    val recommendedRestaurants: LiveData<List<Restaurant>> = RecommendationsRepository.homeRecommendations()

    // PUBLIC_INTERFACE
    fun recommendedMenuItemsForRestaurant(restaurantId: String): LiveData<List<MenuItem>> {
        /** Provide menu recommendations ("You might also like") for a restaurant. */
        return RecommendationsRepository.recommendationsForRestaurantMenu(restaurantId)
    }
}
