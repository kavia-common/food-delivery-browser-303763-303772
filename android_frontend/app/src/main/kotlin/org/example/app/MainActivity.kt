package org.example.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.example.app.ui.cart.CartFragment
import org.example.app.ui.home.HomeFragment
import org.example.app.ui.shared.SharedCartViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var cartBadge: BadgeDrawable
    private lateinit var sharedCartViewModel: SharedCartViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedCartViewModel = ViewModelProvider(this)[SharedCartViewModel::class.java]

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

                R.id.nav_cart -> {
                    navigateCart()
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

    private fun navigateCart() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, CartFragment.newInstance(), CartFragment.TAG)
            .commit()
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
}
