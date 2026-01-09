package org.example.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.example.app.data.cart.CartRepository
import org.example.app.data.delivery.DeliveryRepository
import org.example.app.data.favorites.FavoritesRepository
import org.example.app.data.preferences.AppPreferencesRepository
import org.example.app.ui.cart.CartFragment
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
                    navigateHome()
                    true
                }

                R.id.nav_favorites -> {
                    navigateFavorites()
                    true
                }

                R.id.nav_cart -> {
                    navigateCart()
                    true
                }

                R.id.nav_delivery -> {
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
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment.newInstance(), HomeFragment.TAG)
            .commit()
    }

    private fun navigateFavorites() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, FavoritesFragment.newInstance(), FavoritesFragment.TAG)
            .commit()
    }

    private fun navigateCart() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, CartFragment.newInstance(), CartFragment.TAG)
            .commit()
    }

    private fun navigateDelivery() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, DeliveryFragment.newInstance(), DeliveryFragment.TAG)
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

    /**
     * PUBLIC_INTERFACE
     * Navigate from Home -> Menu for a specific restaurant.
     */
    fun openMenu(restaurantId: String) {
        val fragment = org.example.app.ui.menu.MenuFragment.newInstance(restaurantId)
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
