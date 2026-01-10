package org.example.app.data.recent

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.example.app.data.storage.PreferencesStorage

/**
 * Local-only recently viewed tracker.
 *
 * Stores two bounded MRU lists:
 * - restaurant IDs (when user opens a restaurant menu)
 * - menu item IDs (when user taps an item row / opens options)
 *
 * Backed by SharedPreferences via PreferencesStorage.
 */
object RecentlyViewedRepository {

    private const val MAX_SIZE = 20

    private var storage: PreferencesStorage? = null

    // Most-recent-first ordering.
    private val recentRestaurantIds: ArrayDeque<String> = ArrayDeque()
    private val recentMenuItemIds: ArrayDeque<String> = ArrayDeque()

    private val _recentRestaurantIdsLive = MutableLiveData<List<String>>(emptyList())
    val recentRestaurantIdsLive: LiveData<List<String>> = _recentRestaurantIdsLive

    private val _recentMenuItemIdsLive = MutableLiveData<List<String>>(emptyList())
    val recentMenuItemIdsLive: LiveData<List<String>> = _recentMenuItemIdsLive

    // PUBLIC_INTERFACE
    fun initialize(context: Context) {
        /** Initialize/hydrate from local persistence. Safe to call multiple times. */
        if (storage == null) storage = PreferencesStorage.from(context)
        val st = storage ?: return

        recentRestaurantIds.clear()
        st.loadRecentlyViewedRestaurantIds().forEach { pushMru(recentRestaurantIds, it) }

        recentMenuItemIds.clear()
        st.loadRecentlyViewedMenuItemIds().forEach { pushMru(recentMenuItemIds, it) }

        publish()
    }

    // PUBLIC_INTERFACE
    fun recordRestaurantView(restaurantId: String) {
        /** Record that a restaurant was viewed (opened menu). */
        if (restaurantId.isBlank()) return
        pushMru(recentRestaurantIds, restaurantId)
        persistRestaurants()
        publishRestaurants()
    }

    // PUBLIC_INTERFACE
    fun recordMenuItemView(menuItemId: String) {
        /** Record that a menu item was viewed (tapped/opened options). */
        if (menuItemId.isBlank()) return
        pushMru(recentMenuItemIds, menuItemId)
        persistMenuItems()
        publishMenuItems()
    }

    private fun pushMru(deque: ArrayDeque<String>, id: String) {
        // Remove existing entry if present, then add to front.
        deque.removeAll { it == id }
        deque.addFirst(id)

        while (deque.size > MAX_SIZE) {
            deque.removeLast()
        }
    }

    private fun persistRestaurants() {
        storage?.saveRecentlyViewedRestaurantIds(recentRestaurantIds.toList())
    }

    private fun persistMenuItems() {
        storage?.saveRecentlyViewedMenuItemIds(recentMenuItemIds.toList())
    }

    private fun publishRestaurants() {
        _recentRestaurantIdsLive.value = recentRestaurantIds.toList()
    }

    private fun publishMenuItems() {
        _recentMenuItemIdsLive.value = recentMenuItemIds.toList()
    }

    private fun publish() {
        publishRestaurants()
        publishMenuItems()
    }
}
