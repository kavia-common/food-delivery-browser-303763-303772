package org.example.app.data.models

/**
 * Represents a restaurant shown on the Home screen.
 */
data class Restaurant(
    val id: String,
    val name: String,
    val cuisineTags: List<String>,
    val rating: Double,
    val etaMinutesMin: Int,
    val etaMinutesMax: Int,
    val bannerColorHex: String
)

/**
 * Represents a menu category within a restaurant.
 */
data class MenuCategory(
    val id: String,
    val title: String
)

/**
 * Represents a menu item user can add to cart.
 */
data class MenuItem(
    val id: String,
    val restaurantId: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val priceCents: Int,
    val isVeg: Boolean
)

/**
 * Cart line item (menu item + quantity).
 */
data class CartLine(
    val item: MenuItem,
    val quantity: Int
)
