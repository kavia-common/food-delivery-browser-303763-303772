package org.example.app.data.preferences

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.example.app.data.storage.PreferencesStorage

/**
 * Basic preferences store for lightweight, UI-only persistence.
 *
 * Stores Home screen browsing state:
 * - favorites-only toggle
 * - search query
 * - veg-only chip
 * - selected cuisine chips
 * - selected sort option
 */
object AppPreferencesRepository {

    private var storage: PreferencesStorage? = null

    private const val KEY_HOME_FAVORITES_ONLY = "home_favorites_only"

    private const val KEY_HOME_SEARCH_QUERY = "home_search_query"
    private const val KEY_HOME_VEG_ONLY = "home_veg_only"
    private const val KEY_HOME_SELECTED_CUISINES = "home_selected_cuisines" // encoded as pipe-delimited string
    private const val KEY_HOME_SORT = "home_sort"

    enum class HomeSortOption(val id: String) {
        RATING_DESC("rating_desc"),
        PRICE_ASC("price_asc"),
        ETA_ASC("eta_asc"),
        POPULARITY_DESC("popularity_desc");

        companion object {
            fun fromId(id: String?): HomeSortOption {
                return entries.firstOrNull { it.id == id } ?: RATING_DESC
            }
        }
    }

    private val _homeFavoritesOnly = MutableLiveData(false)
    val homeFavoritesOnly: LiveData<Boolean> = _homeFavoritesOnly

    private val _homeSearchQuery = MutableLiveData("")
    val homeSearchQuery: LiveData<String> = _homeSearchQuery

    private val _homeVegOnly = MutableLiveData(false)
    val homeVegOnly: LiveData<Boolean> = _homeVegOnly

    private val _homeSelectedCuisines = MutableLiveData<Set<String>>(emptySet())
    val homeSelectedCuisines: LiveData<Set<String>> = _homeSelectedCuisines

    private val _homeSortOption = MutableLiveData(HomeSortOption.RATING_DESC)
    val homeSortOption: LiveData<HomeSortOption> = _homeSortOption

    // PUBLIC_INTERFACE
    fun initialize(context: Context) {
        /** Initialize repository with SharedPreferences and load persisted values. */
        if (storage == null) storage = PreferencesStorage.from(context)

        _homeFavoritesOnly.value = storage?.loadPreferenceString(KEY_HOME_FAVORITES_ONLY)?.toBooleanStrictOrNull() ?: false
        _homeSearchQuery.value = storage?.loadPreferenceString(KEY_HOME_SEARCH_QUERY).orEmpty()
        _homeVegOnly.value = storage?.loadPreferenceString(KEY_HOME_VEG_ONLY)?.toBooleanStrictOrNull() ?: false

        val cuisinesEncoded = storage?.loadPreferenceString(KEY_HOME_SELECTED_CUISINES)
        _homeSelectedCuisines.value = decodeCuisineSet(cuisinesEncoded)

        val sortId = storage?.loadPreferenceString(KEY_HOME_SORT)
        _homeSortOption.value = HomeSortOption.fromId(sortId)
    }

    // PUBLIC_INTERFACE
    fun setHomeFavoritesOnly(enabled: Boolean) {
        /** Persist and publish favorites-only filter. */
        _homeFavoritesOnly.value = enabled
        storage?.savePreferenceString(KEY_HOME_FAVORITES_ONLY, enabled.toString())
    }

    // PUBLIC_INTERFACE
    fun setHomeSearchQuery(query: String) {
        /** Persist and publish current Home search query. */
        _homeSearchQuery.value = query
        storage?.savePreferenceString(KEY_HOME_SEARCH_QUERY, query)
    }

    // PUBLIC_INTERFACE
    fun setHomeVegOnly(enabled: Boolean) {
        /** Persist and publish veg-only chip. */
        _homeVegOnly.value = enabled
        storage?.savePreferenceString(KEY_HOME_VEG_ONLY, enabled.toString())
    }

    // PUBLIC_INTERFACE
    fun setHomeSelectedCuisines(cuisines: Set<String>) {
        /** Persist and publish currently-selected cuisine chips. */
        _homeSelectedCuisines.value = cuisines
        storage?.savePreferenceString(KEY_HOME_SELECTED_CUISINES, encodeCuisineSet(cuisines))
    }

    // PUBLIC_INTERFACE
    fun setHomeSortOption(option: HomeSortOption) {
        /** Persist and publish selected sort option. */
        _homeSortOption.value = option
        storage?.savePreferenceString(KEY_HOME_SORT, option.id)
    }

    private fun encodeCuisineSet(values: Set<String>): String {
        // Pipe-delimited list. Cuisines in mock data do not contain '|', so this is safe here.
        return values.joinToString(separator = "|")
    }

    private fun decodeCuisineSet(encoded: String?): Set<String> {
        if (encoded.isNullOrBlank()) return emptySet()
        return encoded.split("|").mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } }.toSet()
    }

    private fun String.toBooleanStrictOrNull(): Boolean? {
        return when (this.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }
}
