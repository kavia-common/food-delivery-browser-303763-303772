package org.example.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import org.example.app.data.storage.models.StoredCartLine

/**
 * SharedPreferences-backed persistence for lightweight local state:
 * - cart contents
 * - favorites (restaurant IDs + menu item IDs)
 * - basic preferences (stub)
 *
 * This is deliberately simple (no Room), resilient to missing/malformed values.
 */
class PreferencesStorage private constructor(
    private val prefs: SharedPreferences
) {

    private fun getString(key: String): String? = prefs.getString(key, null)

    private fun putString(key: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    // PUBLIC_INTERFACE
    fun loadFavoriteRestaurantIds(): Set<String> {
        /** Load favorited restaurant IDs. Defaults to empty on missing/malformed state. */
        return SafeCodec.decodeStringSet(getString(KEY_FAVORITE_RESTAURANTS))
    }

    // PUBLIC_INTERFACE
    fun saveFavoriteRestaurantIds(ids: Set<String>) {
        /** Persist favorited restaurant IDs. */
        putString(KEY_FAVORITE_RESTAURANTS, SafeCodec.encodeStringSet(ids))
    }

    // PUBLIC_INTERFACE
    fun loadFavoriteMenuItemIds(): Set<String> {
        /** Load favorited menu item IDs. Defaults to empty on missing/malformed state. */
        return SafeCodec.decodeStringSet(getString(KEY_FAVORITE_MENU_ITEMS))
    }

    // PUBLIC_INTERFACE
    fun saveFavoriteMenuItemIds(ids: Set<String>) {
        /** Persist favorited menu item IDs. */
        putString(KEY_FAVORITE_MENU_ITEMS, SafeCodec.encodeStringSet(ids))
    }

    // PUBLIC_INTERFACE
    fun loadCartLines(): List<StoredCartLine> {
        /** Load cart lines. Defaults to empty on missing/malformed state. */
        return SafeCodec.decodeCartLines(getString(KEY_CART_LINES))
    }

    // PUBLIC_INTERFACE
    fun saveCartLines(lines: List<StoredCartLine>) {
        /** Persist cart lines. */
        putString(KEY_CART_LINES, SafeCodec.encodeCartLines(lines))
    }

    // PUBLIC_INTERFACE
    fun loadCartPromo(): org.example.app.data.cart.AppliedPromo? {
        /** Load persisted cart promo settings. Returns null when missing/malformed. */
        return SafeCodec.decodeAppliedPromo(getString(KEY_CART_PROMO))
    }

    // PUBLIC_INTERFACE
    fun saveCartPromo(promo: org.example.app.data.cart.AppliedPromo?) {
        /** Persist cart promo settings; null removes. */
        putString(KEY_CART_PROMO, promo?.let { SafeCodec.encodeAppliedPromo(it) })
    }

    // PUBLIC_INTERFACE
    fun loadCartFeeSettings(): org.example.app.data.cart.FeeSettings? {
        /** Load persisted cart fee settings. Returns null when missing/malformed. */
        return SafeCodec.decodeFeeSettings(getString(KEY_CART_FEE_SETTINGS))
    }

    // PUBLIC_INTERFACE
    fun saveCartFeeSettings(settings: org.example.app.data.cart.FeeSettings?) {
        /** Persist cart fee settings; null removes. */
        putString(KEY_CART_FEE_SETTINGS, settings?.let { SafeCodec.encodeFeeSettings(it) })
    }

    // PUBLIC_INTERFACE
    fun loadPreferenceString(key: String): String? {
        /** Stub: load a preference string for future use. */
        return getString(PREF_PREFIX + key)
    }

    // PUBLIC_INTERFACE
    fun savePreferenceString(key: String, value: String?) {
        /** Stub: save a preference string for future use. */
        putString(PREF_PREFIX + key, value)
    }

    // PUBLIC_INTERFACE
    fun loadActiveDeliveryOrderEncoded(): String? {
        /** Load persisted active delivery order state (opaque encoded string). */
        return getString(KEY_DELIVERY_ACTIVE_ORDER)
    }

    // PUBLIC_INTERFACE
    fun saveActiveDeliveryOrderEncoded(encoded: String?) {
        /** Persist active delivery order state; null removes. */
        putString(KEY_DELIVERY_ACTIVE_ORDER, encoded)
    }

    companion object {
        private const val PREFS_NAME = "food_delivery_prefs"

        private const val KEY_FAVORITE_RESTAURANTS = "favorites_restaurants_v1"
        private const val KEY_FAVORITE_MENU_ITEMS = "favorites_menu_items_v1"
        private const val KEY_CART_LINES = "cart_lines_v1"

        // Cart pricing persistence.
        private const val KEY_CART_PROMO = "cart_promo_v1"
        private const val KEY_CART_FEE_SETTINGS = "cart_fee_settings_v1"

        // Delivery simulation persistence.
        private const val KEY_DELIVERY_ACTIVE_ORDER = "delivery_active_order_v1"

        private const val PREF_PREFIX = "pref_"

        // PUBLIC_INTERFACE
        fun from(context: Context): PreferencesStorage {
            /** Factory: create storage using app-private SharedPreferences. */
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return PreferencesStorage(prefs)
        }
    }
}
