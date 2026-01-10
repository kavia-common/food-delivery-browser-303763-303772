package org.example.app

import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.example.app.data.cart.CartRepository
import org.example.app.data.delivery.DeliveryRepository
import org.example.app.data.favorites.FavoritesRepository
import org.example.app.data.preferences.AppPreferencesRepository
import org.example.app.ui.cart.CartFragment
import org.example.app.ui.common.MotionUtils
import org.example.app.ui.delivery.DeliveryFragment
import org.example.app.ui.favorites.FavoritesFragment
import org.example.app.ui.home.HomeFragment
import org.example.app.ui.shared.SharedAppViewModel
import org.example.app.ui.shared.SharedCartViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var cartBadge: BadgeDrawable
    private lateinit var sharedCartViewModel: SharedCartViewModel
    private lateinit var sharedAppViewModel: SharedAppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hydrate repositories from local persistence on app start.
        CartRepository.initialize(this)
        FavoritesRepository.initialize(this)
        AppPreferencesRepository.initialize(this)
        DeliveryRepository.initialize(this)

        // Apply the persisted theme mode as early as possible.
        applyThemeMode(AppPreferencesRepository.themeMode.value ?: AppPreferencesRepository.ThemeMode.SYSTEM)

        setContentView(R.layout.activity_main)

        sharedCartViewModel = ViewModelProvider(this)[SharedCartViewModel::class.java]
        sharedAppViewModel = ViewModelProvider(this)[SharedAppViewModel::class.java]

        // React to changes without restart.
        sharedAppViewModel.themeMode.observe(this) { mode ->
            applyThemeMode(mode)
        }

        bottomNav = findViewById(R.id.bottomNav)

        cartBadge = bottomNav.getOrCreateBadge(R.id.nav_cart)
        cartBadge.isVisible = false

        sharedCartViewModel.totalItemCount.observe(this) { count ->
            cartBadge.isVisible = count > 0
            if (count > 0) cartBadge.number = count
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    animateBottomNavSelection(item.itemId)
                    navigateHome()
                    true
                }

                R.id.nav_favorites -> {
                    animateBottomNavSelection(item.itemId)
                    navigateFavorites()
                    true
                }

                R.id.nav_cart -> {
                    animateBottomNavSelection(item.itemId)
                    navigateCart()
                    true
                }

                R.id.nav_delivery -> {
                    animateBottomNavSelection(item.itemId)
                    navigateDelivery()
                    true
                }

                else -> false
            }
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
            navigateHome()
        }
    }

    private fun navigateHome() {
        val fragment = HomeFragment.newInstance()
        applyMaterialMotion(fragment, isTopLevel = true)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, HomeFragment.TAG)
            .commit()
    }

    private fun navigateFavorites() {
        val fragment = FavoritesFragment.newInstance()
        applyMaterialMotion(fragment, isTopLevel = true)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, FavoritesFragment.TAG)
            .commit()
    }

    private fun navigateCart() {
        val fragment = CartFragment.newInstance()
        applyMaterialMotion(fragment, isTopLevel = true)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, CartFragment.TAG)
            .commit()
    }

    private fun navigateDelivery() {
        val fragment = DeliveryFragment.newInstance()
        applyMaterialMotion(fragment, isTopLevel = true)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, DeliveryFragment.TAG)
            .commit()
    }

    private fun applyThemeMode(mode: AppPreferencesRepository.ThemeMode) {
        val nightMode = when (mode) {
            AppPreferencesRepository.ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppPreferencesRepository.ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppPreferencesRepository.ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun applyMaterialMotion(fragment: androidx.fragment.app.Fragment, isTopLevel: Boolean) {
        if (MotionUtils.isReducedMotionEnabled(this)) return

        // Use FadeThrough for top-level destination swaps, SharedAxis for drill-in/out.
        if (isTopLevel) {
            val enter = com.google.android.material.transition.MaterialFadeThrough().apply { duration = 180L }
            val exit = com.google.android.material.transition.MaterialFadeThrough().apply { duration = 160L }
            fragment.enterTransition = enter
            fragment.exitTransition = exit
            fragment.reenterTransition = enter
            fragment.returnTransition = exit
        } else {
            val forward = com.google.android.material.transition.MaterialSharedAxis(
                com.google.android.material.transition.MaterialSharedAxis.X,
                /* forward= */ true
            ).apply { duration = 200L }
            val backward = com.google.android.material.transition.MaterialSharedAxis(
                com.google.android.material.transition.MaterialSharedAxis.X,
                /* forward= */ false
            ).apply { duration = 200L }
            fragment.enterTransition = forward
            fragment.returnTransition = backward
            fragment.reenterTransition = backward
            fragment.exitTransition = forward
        }
    }

    private fun animateBottomNavSelection(selectedItemId: Int) {
        if (MotionUtils.isReducedMotionEnabled(this)) return

        // BottomNavigationView doesn't expose a built-in selection animation.
        // Keep this implementation lint-safe by not referencing restricted/internal Material classes.
        fun collectItemViews(root: ViewGroup, out: MutableList<android.view.View>) {
            for (child in root.children) {
                // Bottom nav items are clickable containers; we only care about the first level under menu view.
                if (child.isClickable) out.add(child)
                if (child is ViewGroup) collectItemViews(child, out)
            }
        }

        // Heuristic: the menu container is the first child ViewGroup, and its direct children are item views.
        val menuContainer = (bottomNav.getChildAt(0) as? ViewGroup) ?: return
        val itemViews = menuContainer.children.toList()

        itemViews.forEach { itemView ->
            val isSelected = itemView.id == selectedItemId

            val targetScale = if (isSelected) 1.06f else 1.0f
            val targetAlpha = if (isSelected) 1.0f else 0.88f

            fun animateView(v: android.view.View?) {
                if (v == null) return
                v.animate().cancel()
                v.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .alpha(targetAlpha)
                    .setDuration(140L)
                    .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                    .start()
            }

            // Icon has a stable id in Material resources.
            val icon = itemView.findViewById<android.view.View>(com.google.android.material.R.id.icon)
            animateView(icon)

            // Labels are TextViews somewhere within the item view hierarchy.
            fun collectTextViews(root: ViewGroup, out: MutableList<TextView>) {
                for (child in root.children) {
                    when (child) {
                        is TextView -> out.add(child)
                        is ViewGroup -> collectTextViews(child, out)
                    }
                }
            }

            val labels = mutableListOf<TextView>()
            (itemView as? ViewGroup)?.let { collectTextViews(it, labels) }
            labels.forEach { animateView(it) }
        }
    }

    /**
     * PUBLIC_INTERFACE
     * Navigate from Home -> Menu for a specific restaurant.
     */
    fun openMenu(restaurantId: String) {
        val fragment = org.example.app.ui.menu.MenuFragment.newInstance(restaurantId)
        applyMaterialMotion(fragment, isTopLevel = false)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, org.example.app.ui.menu.MenuFragment.TAG)
            .addToBackStack(org.example.app.ui.menu.MenuFragment.TAG)
            .commit()

        bottomNav.isVisible = true
    }

    /**
     * PUBLIC_INTERFACE
     * Select cart tab programmatically.
     */
    fun openCartTab() {
        bottomNav.selectedItemId = R.id.nav_cart
        navigateCart()
    }

    /**
     * PUBLIC_INTERFACE
     * Navigate to Delivery tracking screen/tab.
     */
    fun openDelivery() {
        bottomNav.selectedItemId = R.id.nav_delivery
        navigateDelivery()
    }
}
