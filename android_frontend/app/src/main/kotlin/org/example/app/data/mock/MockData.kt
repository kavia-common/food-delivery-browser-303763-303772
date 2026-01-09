package org.example.app.data.mock

import org.example.app.data.models.MenuCategory
import org.example.app.data.models.MenuItem
import org.example.app.data.models.Restaurant

object MockData {

    val restaurants: List<Restaurant> = listOf(
        Restaurant("r1", "Blue Bowl Kitchen", listOf("Healthy", "Bowls", "Salads"), 4.6, 25, 35, "#DBEAFE"),
        Restaurant("r2", "Spice Route", listOf("Indian", "Curry", "Biryani"), 4.3, 30, 45, "#DCFCE7"),
        Restaurant("r3", "Sushi Station", listOf("Japanese", "Sushi", "Bento"), 4.5, 20, 35, "#FFE4E6"),
        Restaurant("r4", "Pasta & Co.", listOf("Italian", "Pasta", "Pizza"), 4.2, 25, 40, "#FEF3C7"),
        Restaurant("r5", "Taco Town", listOf("Mexican", "Tacos", "Burritos"), 4.4, 20, 30, "#E0F2FE"),
        Restaurant("r6", "Burger Bureau", listOf("Burgers", "Fries", "Shakes"), 4.1, 20, 35, "#F3E8FF"),
        Restaurant("r7", "Green Garden", listOf("Vegetarian", "Vegan", "Fresh"), 4.7, 25, 40, "#ECFCCB"),
        Restaurant("r8", "Sweet Studio", listOf("Desserts", "Cakes", "Ice Cream"), 4.5, 25, 40, "#FFE4E6"),
        Restaurant("r9", "Wok Waves", listOf("Chinese", "Noodles", "Stir-fry"), 4.2, 25, 40, "#E5E7EB"),
        Restaurant("r10", "Mediterraneo", listOf("Mediterranean", "Kebab", "Bowls"), 4.4, 25, 40, "#DBEAFE")
    )

    val categories: List<MenuCategory> = listOf(
        MenuCategory("c_popular", "Popular"),
        MenuCategory("c_starters", "Starters"),
        MenuCategory("c_mains", "Mains"),
        MenuCategory("c_desserts", "Desserts"),
        MenuCategory("c_drinks", "Drinks")
    )

    fun menuForRestaurant(restaurantId: String): List<MenuItem> {
        fun id(suffix: String) = "${restaurantId}_$suffix"

        return listOf(
            MenuItem(id("popular_1"), restaurantId, "c_popular", "Signature Bowl", "House favorite with seasonal toppings.", 1099, true),
            MenuItem(id("popular_2"), restaurantId, "c_popular", "Chef’s Special Wrap", "Toasted wrap with sauce and crunch.", 999, false),

            MenuItem(id("starter_1"), restaurantId, "c_starters", "Crispy Fries", "Sea salt, served with dip.", 399, true),
            MenuItem(id("starter_2"), restaurantId, "c_starters", "Spicy Wings", "Glazed, tangy, and hot.", 699, false),

            MenuItem(id("main_1"), restaurantId, "c_mains", "Classic Burger", "Juicy patty, lettuce, and house sauce.", 1199, false),
            MenuItem(id("main_2"), restaurantId, "c_mains", "Paneer Tikka Plate", "Charred paneer with herbs and salad.", 1299, true),

            MenuItem(id("dessert_1"), restaurantId, "c_desserts", "Chocolate Brownie", "Warm brownie with cocoa.", 499, true),
            MenuItem(id("dessert_2"), restaurantId, "c_desserts", "Cheesecake Slice", "Creamy and smooth.", 549, true),

            MenuItem(id("drink_1"), restaurantId, "c_drinks", "Iced Tea", "Freshly brewed, lightly sweet.", 249, true),
            MenuItem(id("drink_2"), restaurantId, "c_drinks", "Sparkling Water", "Chilled and crisp.", 199, true)
        )
    }

    fun restaurantById(id: String): Restaurant? = restaurants.firstOrNull { it.id == id }
}
