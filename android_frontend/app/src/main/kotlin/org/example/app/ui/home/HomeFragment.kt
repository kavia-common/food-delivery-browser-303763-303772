package org.example.app.ui.home

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import androidx.core.view.isVisible
import org.example.app.ui.common.MotionUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.example.app.MainActivity
import org.example.app.R
import org.example.app.data.delivery.DeliveryRepository
import org.example.app.data.favorites.FavoritesRepository
import org.example.app.data.mock.MockData
import org.example.app.data.models.Restaurant
import org.example.app.data.preferences.AppPreferencesRepository
import org.example.app.ui.shared.SharedAppViewModel
import kotlin.math.roundToInt

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RestaurantAdapter

    private lateinit var favoritesOnlySwitch: SwitchMaterial
    private lateinit var searchInputLayout: TextInputLayout
    private lateinit var searchEditText: TextInputEditText
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var sortButton: MaterialButton
    private lateinit var deliveryEtaBadge: TextView

    private lateinit var sharedAppViewModel: SharedAppViewModel

    private lateinit var recommendedSection: View
    private lateinit var recommendedRecyclerView: RecyclerView
    private lateinit var recommendedAdapter: RecommendedRestaurantAdapter

    private val allRestaurants: List<Restaurant> = MockData.restaurants

    override fun onAttach(context: Context) {
        super.onAttach(context)
        adapter = RestaurantAdapter(
            onClick = { restaurant -> (activity as? MainActivity)?.openMenu(restaurant.id) },
            isFavorited = { id -> FavoritesRepository.isRestaurantFavorited(id) },
            onToggleFavorite = { id -> FavoritesRepository.toggleRestaurantFavorite(id) }
        )

        recommendedAdapter = RecommendedRestaurantAdapter(
            onClick = { restaurant -> (activity as? MainActivity)?.openMenu(restaurant.id) }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedAppViewModel = ViewModelProvider(requireActivity())[SharedAppViewModel::class.java]
        setHasOptionsMenu(true)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.home_overflow_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_theme -> {
                showThemeDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.restaurantRecyclerView)
        favoritesOnlySwitch = view.findViewById(R.id.favoritesOnlySwitch)
        searchInputLayout = view.findViewById(R.id.searchInputLayout)
        searchEditText = view.findViewById(R.id.searchEditText)
        filterChipGroup = view.findViewById(R.id.filterChipGroup)
        sortButton = view.findViewById(R.id.sortButton)
        deliveryEtaBadge = view.findViewById(R.id.homeDeliveryEtaBadge)

        recommendedSection = view.findViewById(R.id.recommendedSection)
        recommendedRecyclerView = view.findViewById(R.id.recommendedRecyclerView)

        recommendedRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recommendedRecyclerView.adapter = recommendedAdapter
        recommendedRecyclerView.itemAnimator = MotionUtils.createSubtleItemAnimator(requireContext())

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = MotionUtils.createSubtleItemAnimator(requireContext())

        setupFilterChips()
        setupSearch()
        setupSort()

        // Restore favorites-only switch state.
        favoritesOnlySwitch.setOnCheckedChangeListener(null)
        favoritesOnlySwitch.isChecked = sharedAppViewModel.homeFavoritesOnly.value ?: false
        favoritesOnlySwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            AppPreferencesRepository.setHomeFavoritesOnly(isChecked)
        }

        fun render() {
            val favoritesOnly = sharedAppViewModel.homeFavoritesOnly.value ?: false
            val favIds = sharedAppViewModel.favoriteRestaurantIds.value ?: emptySet()

            val query = sharedAppViewModel.homeSearchQuery.value.orEmpty()
            val vegOnly = sharedAppViewModel.homeVegOnly.value ?: false
            val selectedCuisines = sharedAppViewModel.homeSelectedCuisines.value ?: emptySet()
            val sort = sharedAppViewModel.homeSortOption.value ?: AppPreferencesRepository.HomeSortOption.RATING_DESC

            val base = if (favoritesOnly) allRestaurants.filter { favIds.contains(it.id) } else allRestaurants
            val filtered = applySearchAndFilters(base, query, vegOnly, selectedCuisines)
            val sorted = applySort(filtered, sort, favIds)

            adapter.submitList(sorted)

            // Keep sort button text in sync with selection.
            sortButton.text = getSortLabel(sort)
        }

        // Recommendations carousel (independent of filters/sort on the main list).
        sharedAppViewModel.recommendedRestaurants.observe(viewLifecycleOwner) { recs ->
            val list = (recs ?: emptyList()).take(10)
            recommendedAdapter.submitList(list)
            recommendedSection.isVisible = list.isNotEmpty()
        }

        // Observe changes and re-render.
        sharedAppViewModel.favoriteRestaurantIds.observe(viewLifecycleOwner) {
            render()
            // Also refresh heart icons.
            adapter.notifyDataSetChanged()
        }
        sharedAppViewModel.homeFavoritesOnly.observe(viewLifecycleOwner) { render() }
        sharedAppViewModel.homeSearchQuery.observe(viewLifecycleOwner) { render() }
        sharedAppViewModel.homeVegOnly.observe(viewLifecycleOwner) { render() }
        sharedAppViewModel.homeSelectedCuisines.observe(viewLifecycleOwner) { render() }
        sharedAppViewModel.homeSortOption.observe(viewLifecycleOwner) { render() }

        // Delivery badge: initialize repo and observe (safe; no-op if already initialized).
        DeliveryRepository.initialize(requireContext())
        DeliveryRepository.activeOrder.observe(viewLifecycleOwner) { order ->
            deliveryEtaBadge.isVisible = order != null && order.currentStage != org.example.app.data.delivery.DeliveryStage.DELIVERED
        }
        DeliveryRepository.etaRemainingMs.observe(viewLifecycleOwner) { remaining ->
            val order = DeliveryRepository.activeOrder.value
            val show = order != null && order.currentStage != org.example.app.data.delivery.DeliveryStage.DELIVERED
            deliveryEtaBadge.isVisible = show
            if (show) {
                val sec = ((remaining ?: 0L) / 1000L).coerceAtLeast(0L)
                val text = if (sec >= 60) {
                    val m = sec / 60
                    val s = sec % 60
                    getString(R.string.delivery_eta_badge, "${m}m ${s}s")
                } else {
                    getString(R.string.delivery_eta_badge, "${sec}s")
                }
                deliveryEtaBadge.text = text
            }
        }

        // Initial render after wiring everything.
        restoreUiFromPreferences()
        render()
    }

    private fun setupSearch() {
        // Restore happens via restoreUiFromPreferences() to avoid triggering persistence loops.
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                AppPreferencesRepository.setHomeSearchQuery(s?.toString().orEmpty())
            }
        })
    }

    private fun setupFilterChips() {
        filterChipGroup.removeAllViews()

        // We treat "Vegetarian" and "Vegan" tags as veg-friendly cuisines for the veg-only toggle.
        val vegFriendlyCuisineTags = setOf("Vegetarian", "Vegan")

        val allCuisineTags: List<String> = allRestaurants
            .flatMap { it.cuisineTags }
            .distinct()
            .sorted()

        // Veg-only chip first.
        val vegChip = layoutInflater.inflate(R.layout.item_category_chip, filterChipGroup, false) as Chip
        vegChip.text = getString(R.string.filters_veg_only)
        vegChip.isCheckable = true
        vegChip.setOnCheckedChangeListener { _, isChecked ->
            AppPreferencesRepository.setHomeVegOnly(isChecked)
        }
        filterChipGroup.addView(vegChip)

        // Cuisine chips (multi-select).
        allCuisineTags.forEach { tag ->
            val chip = layoutInflater.inflate(R.layout.item_category_chip, filterChipGroup, false) as Chip
            chip.text = tag
            chip.isCheckable = true
            chip.setOnCheckedChangeListener { _, isChecked ->
                val existing = sharedAppViewModel.homeSelectedCuisines.value ?: emptySet()
                val updated = existing.toMutableSet()
                if (isChecked) updated.add(tag) else updated.remove(tag)
                AppPreferencesRepository.setHomeSelectedCuisines(updated)
            }
            filterChipGroup.addView(chip)
        }

        // Also: if veg-only is enabled, we still allow cuisine chips; veg-only is an additional constraint:
        // restaurant must contain at least one veg-friendly tag.
        // (No extra UI needed here; logic handled in applySearchAndFilters.)
    }

    private fun setupSort() {
        sortButton.setOnClickListener {
            showSortDialog()
        }
    }

    private fun showSortDialog() {
        val options = listOf(
            AppPreferencesRepository.HomeSortOption.RATING_DESC,
            AppPreferencesRepository.HomeSortOption.PRICE_ASC,
            AppPreferencesRepository.HomeSortOption.ETA_ASC,
            AppPreferencesRepository.HomeSortOption.POPULARITY_DESC
        )
        val labels = options.map { getSortLabel(it) }.toTypedArray()

        val current = sharedAppViewModel.homeSortOption.value ?: AppPreferencesRepository.HomeSortOption.RATING_DESC
        val checkedIndex = options.indexOf(current).coerceAtLeast(0)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.sort_by))
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val picked = options.getOrNull(which) ?: return@setSingleChoiceItems
                AppPreferencesRepository.setHomeSortOption(picked)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showThemeDialog() {
        val options = listOf(
            AppPreferencesRepository.ThemeMode.SYSTEM,
            AppPreferencesRepository.ThemeMode.LIGHT,
            AppPreferencesRepository.ThemeMode.DARK
        )
        val labels = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )

        val current = sharedAppViewModel.themeMode.value ?: AppPreferencesRepository.ThemeMode.SYSTEM
        val checkedIndex = options.indexOf(current).coerceAtLeast(0)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.theme))
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val picked = options.getOrNull(which) ?: return@setSingleChoiceItems
                AppPreferencesRepository.setThemeMode(picked)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun getSortLabel(option: AppPreferencesRepository.HomeSortOption): String {
        return when (option) {
            AppPreferencesRepository.HomeSortOption.RATING_DESC -> getString(R.string.sort_rating_desc)
            AppPreferencesRepository.HomeSortOption.PRICE_ASC -> getString(R.string.sort_price_asc)
            AppPreferencesRepository.HomeSortOption.ETA_ASC -> getString(R.string.sort_eta_asc)
            AppPreferencesRepository.HomeSortOption.POPULARITY_DESC -> getString(R.string.sort_popularity_desc)
        }
    }

    private fun restoreUiFromPreferences() {
        // Search text
        val query = sharedAppViewModel.homeSearchQuery.value.orEmpty()
        if (searchEditText.text?.toString() != query) {
            searchEditText.setText(query)
            searchEditText.setSelection(query.length)
        }

        // Chips state
        val vegOnly = sharedAppViewModel.homeVegOnly.value ?: false
        val selectedCuisines = sharedAppViewModel.homeSelectedCuisines.value ?: emptySet()

        // ChipGroup children are: [vegChip, cuisineChip...]
        for (i in 0 until filterChipGroup.childCount) {
            val child = filterChipGroup.getChildAt(i)
            val chip = child as? Chip ?: continue
            if (i == 0) {
                chip.setOnCheckedChangeListener(null)
                chip.isChecked = vegOnly
                chip.setOnCheckedChangeListener { _, isChecked -> AppPreferencesRepository.setHomeVegOnly(isChecked) }
            } else {
                val tag = chip.text?.toString().orEmpty()
                chip.setOnCheckedChangeListener(null)
                chip.isChecked = selectedCuisines.contains(tag)
                chip.setOnCheckedChangeListener { _, isChecked ->
                    val existing = sharedAppViewModel.homeSelectedCuisines.value ?: emptySet()
                    val updated = existing.toMutableSet()
                    if (isChecked) updated.add(tag) else updated.remove(tag)
                    AppPreferencesRepository.setHomeSelectedCuisines(updated)
                }
            }
        }

        // Sort button label updated during render()
    }

    private fun applySearchAndFilters(
        input: List<Restaurant>,
        query: String,
        vegOnly: Boolean,
        selectedCuisines: Set<String>
    ): List<Restaurant> {
        val normalizedQuery = query.trim().lowercase()
        val vegFriendlyCuisineTags = setOf("Vegetarian", "Vegan")

        return input.filter { r ->
            val matchesQuery = if (normalizedQuery.isEmpty()) {
                true
            } else {
                val name = r.name.lowercase()
                val tags = r.cuisineTags.joinToString(" ").lowercase()
                name.contains(normalizedQuery) || tags.contains(normalizedQuery)
            }

            val matchesVeg = if (!vegOnly) {
                true
            } else {
                r.cuisineTags.any { vegFriendlyCuisineTags.contains(it) }
            }

            val matchesCuisineChips = if (selectedCuisines.isEmpty()) {
                true
            } else {
                r.cuisineTags.any { selectedCuisines.contains(it) }
            }

            matchesQuery && matchesVeg && matchesCuisineChips
        }
    }

    private fun applySort(
        input: List<Restaurant>,
        sort: AppPreferencesRepository.HomeSortOption,
        favoriteRestaurantIds: Set<String>
    ): List<Restaurant> {
        // Mock "price" heuristic: average menu item price for restaurant
        fun averageMenuPriceCents(restaurantId: String): Int {
            val menu = MockData.menuForRestaurant(restaurantId)
            if (menu.isEmpty()) return Int.MAX_VALUE
            return (menu.map { it.priceCents }.average()).roundToInt()
        }

        // Mock "popularity": favorited restaurants first, then higher rating as tiebreaker.
        fun popularityScore(r: Restaurant): Int {
            val favBoost = if (favoriteRestaurantIds.contains(r.id)) 1000 else 0
            val ratingScore = (r.rating * 100.0).roundToInt()
            return favBoost + ratingScore
        }

        return when (sort) {
            AppPreferencesRepository.HomeSortOption.RATING_DESC ->
                input.sortedWith(compareByDescending<Restaurant> { it.rating }.thenBy { it.name })

            AppPreferencesRepository.HomeSortOption.PRICE_ASC ->
                input.sortedWith(compareBy<Restaurant> { averageMenuPriceCents(it.id) }.thenBy { it.name })

            AppPreferencesRepository.HomeSortOption.ETA_ASC ->
                input.sortedWith(compareBy<Restaurant> { it.etaMinutesMin }.thenBy { it.name })

            AppPreferencesRepository.HomeSortOption.POPULARITY_DESC ->
                input.sortedWith(compareByDescending<Restaurant> { popularityScore(it) }.thenByDescending { it.rating }.thenBy { it.name })
        }
    }

    companion object {
        const val TAG = "HomeFragment"

        fun newInstance(): HomeFragment = HomeFragment()
    }
}
