package org.example.app.data.preferences

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.example.app.data.storage.PreferencesStorage

/**
 * Basic preferences store (stub) to support future sort/filter persistence.
 * Currently stores a single "home_filter_favorites_only" boolean.
 */
object AppPreferencesRepository {

    private var storage: PreferencesStorage? = null

    private const val KEY_HOME_FAVORITES_ONLY = "home_favorites_only"

    private val _homeFavoritesOnly = MutableLiveData(false)
    val homeFavoritesOnly: LiveData<Boolean> = _homeFavoritesOnly

    // PUBLIC_INTERFACE
    fun initialize(context: Context) {
        /** Initialize repository with SharedPreferences and load persisted values. */
        if (storage == null) storage = PreferencesStorage.from(context)
        val stored = storage?.loadPreferenceString(KEY_HOME_FAVORITES_ONLY)
        _homeFavoritesOnly.value = stored?.toBooleanStrictOrNull() ?: false
    }

    // PUBLIC_INTERFACE
    fun setHomeFavoritesOnly(enabled: Boolean) {
        /** Persist and publish favorites-only filter. */
        _homeFavoritesOnly.value = enabled
        storage?.savePreferenceString(KEY_HOME_FAVORITES_ONLY, enabled.toString())
    }

    private fun String.toBooleanStrictOrNull(): Boolean? {
        return when (this.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }
}
