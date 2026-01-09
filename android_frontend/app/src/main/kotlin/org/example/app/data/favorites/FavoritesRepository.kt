package org.example.app.data.favorites

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.example.app.data.storage.PreferencesStorage

/**
 * In-memory favorites repository with local persistence.
 *
 * Use initialize(context) once on app start to load persisted state.
 */
object FavoritesRepository {

    private var storage: PreferencesStorage? = null

    private val favoriteRestaurantIds: LinkedHashSet<String> = LinkedHashSet()
    private val favoriteMenuItemIds: LinkedHashSet<String> = LinkedHashSet()

    private val _favoriteRestaurantIdsLive = MutableLiveData<Set<String>>(emptySet())
    val favoriteRestaurantIdsLive: LiveData<Set<String>> = _favoriteRestaurantIdsLive

    private val _favoriteMenuItemIdsLive = MutableLiveData<Set<String>>(emptySet())
    val favoriteMenuItemIdsLive: LiveData<Set<String>> = _favoriteMenuItemIdsLive

    // PUBLIC_INTERFACE
    fun initialize(context: Context) {
        /** Initialize from SharedPreferences. Safe to call multiple times. */
        if (storage == null) storage = PreferencesStorage.from(context)

        val st = storage ?: return
        favoriteRestaurantIds.clear()
        favoriteRestaurantIds.addAll(st.loadFavoriteRestaurantIds())

        favoriteMenuItemIds.clear()
        favoriteMenuItemIds.addAll(st.loadFavoriteMenuItemIds())

        publish()
    }

    // PUBLIC_INTERFACE
    fun isRestaurantFavorited(restaurantId: String): Boolean {
        /** Check if a restaurant is favorited. */
        return favoriteRestaurantIds.contains(restaurantId)
    }

    // PUBLIC_INTERFACE
    fun toggleRestaurantFavorite(restaurantId: String) {
        /** Toggle restaurant favorite and persist immediately. */
        if (favoriteRestaurantIds.contains(restaurantId)) favoriteRestaurantIds.remove(restaurantId) else favoriteRestaurantIds.add(
            restaurantId
        )
        persistRestaurants()
        publish()
    }

    // PUBLIC_INTERFACE
    fun isMenuItemFavorited(menuItemId: String): Boolean {
        /** Check if a menu item is favorited. */
        return favoriteMenuItemIds.contains(menuItemId)
    }

    // PUBLIC_INTERFACE
    fun toggleMenuItemFavorite(menuItemId: String) {
        /** Toggle menu item favorite and persist immediately. */
        if (favoriteMenuItemIds.contains(menuItemId)) favoriteMenuItemIds.remove(menuItemId) else favoriteMenuItemIds.add(
            menuItemId
        )
        persistMenuItems()
        publish()
    }

    private fun persistRestaurants() {
        storage?.saveFavoriteRestaurantIds(favoriteRestaurantIds)
    }

    private fun persistMenuItems() {
        storage?.saveFavoriteMenuItemIds(favoriteMenuItemIds)
    }

    private fun publish() {
        _favoriteRestaurantIdsLive.value = favoriteRestaurantIds.toSet()
        _favoriteMenuItemIdsLive.value = favoriteMenuItemIds.toSet()
    }
}
