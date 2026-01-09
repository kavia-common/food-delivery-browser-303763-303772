package org.example.app.ui.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.example.app.data.favorites.FavoritesRepository
import org.example.app.data.preferences.AppPreferencesRepository

class SharedAppViewModel : ViewModel() {
    val favoriteRestaurantIds: LiveData<Set<String>> = FavoritesRepository.favoriteRestaurantIdsLive
    val favoriteMenuItemIds: LiveData<Set<String>> = FavoritesRepository.favoriteMenuItemIdsLive

    val homeFavoritesOnly: LiveData<Boolean> = AppPreferencesRepository.homeFavoritesOnly
    val homeSearchQuery: LiveData<String> = AppPreferencesRepository.homeSearchQuery
    val homeVegOnly: LiveData<Boolean> = AppPreferencesRepository.homeVegOnly
    val homeSelectedCuisines: LiveData<Set<String>> = AppPreferencesRepository.homeSelectedCuisines
    val homeSortOption: LiveData<AppPreferencesRepository.HomeSortOption> = AppPreferencesRepository.homeSortOption
}
