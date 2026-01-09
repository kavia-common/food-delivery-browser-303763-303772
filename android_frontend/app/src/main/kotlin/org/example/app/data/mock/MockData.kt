package org.example.app.data.mock

import org.example.app.data.models.AddOnGroup
import org.example.app.data.models.MenuCategory
import org.example.app.data.models.MenuItem
import org.example.app.data.models.OptionItem
import org.example.app.data.models.Restaurant
import org.example.app.data.models.VariantGroup

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
        fun oid(itemSuffix: String, optSuffix: String) = "${id(itemSuffix)}_$optSuffix"

        // Example variant + add-on groups used on a few items.
        val bowlSizeGroup = VariantGroup(
            id = oid("popular_1", "vg_size"),
            title = "Size",
            required = true,
            options = listOf(
                OptionItem(id = oid("popular_1", "size_regular"), title = "Regular", priceDeltaCents = 0),
                OptionItem(id = oid("popular_1", "size_large"), title = "Large", priceDeltaCents = 250)
            )
        )

        val bowlProteinAddOns = AddOnGroup(
            id = oid("popular_1", "ag_protein"),
            title = "Extras",
            required = false,
            minSelections = 0,
            maxSelections = 3,
            options = listOf(
                OptionItem(id = oid("popular_1", "extra_avocado"), title = "Add avocado", priceDeltaCents = 150),
                OptionItem(id = oid("popular_1", "extra_chicken"), title = "Add chicken", priceDeltaCents = 250),
                OptionItem(id = oid("popular_1", "extra_egg"), title = "Add egg", priceDeltaCents = 100)
            )
        )

        val wingsSpiceGroup = VariantGroup(
            id = oid("starter_2", "vg_spice"),
            title = "Spice",
            required = true,
            options = listOf(
                OptionItem(id = oid("starter_2", "spice_mild"), title = "Mild", priceDeltaCents = 0),
                OptionItem(id = oid("starter_2", "spice_hot"), title = "Hot", priceDeltaCents = 0),
                OptionItem(id = oid("starter_2", "spice_extra_hot"), title = "Extra hot", priceDeltaCents = 50)
            )
        )

        val wingsSauceAddOns = AddOnGroup(
            id = oid("starter_2", "ag_sauce"),
            title = "Sauce",
            required = false,
            minSelections = 0,
            maxSelections = 2,
            options = listOf(
                OptionItem(id = oid("starter_2", "sauce_ranch"), title = "Ranch dip", priceDeltaCents = 75),
                OptionItem(id = oid("starter_2", "sauce_bbq"), title = "BBQ dip", priceDeltaCents = 75),
                OptionItem(id = oid("starter_2", "sauce_blue_cheese"), title = "Blue cheese dip", priceDeltaCents = 100)
            )
        )

        val burgerCookGroup = VariantGroup(
            id = oid("main_1", "vg_cook"),
            title = "Cook",
            required = true,
            options = listOf(
                OptionItem(id = oid("main_1", "cook_medium"), title = "Medium", priceDeltaCents = 0),
                OptionItem(id = oid("main_1", "cook_well"), title = "Well done", priceDeltaCents = 0)
            )
        )

        val burgerAddOns = AddOnGroup(
            id = oid("main_1", "ag_toppings"),
            title = "Toppings",
            required = false,
            minSelections = 0,
            maxSelections = 4,
            options = listOf(
                OptionItem(id = oid("main_1", "top_cheese"), title = "Cheese", priceDeltaCents = 100),
                OptionItem(id = oid("main_1", "top_bacon"), title = "Bacon", priceDeltaCents = 200),
                OptionItem(id = oid("main_1", "top_jalapeno"), title = "Jalapeño", priceDeltaCents = 50),
                OptionItem(id = oid("main_1", "top_mushrooms"), title = "Mushrooms", priceDeltaCents = 75)
            )
        )

        return listOf(
            MenuItem(
                id("popular_1"),
                restaurantId,
                "c_popular",
                "Signature Bowl",
                "House favorite with seasonal toppings.",
                1099,
                true,
                variantGroups = listOf(bowlSizeGroup),
                addOnGroups = listOf(bowlProteinAddOns)
            ),
            MenuItem(
                id("popular_2"),
                restaurantId,
                "c_popular",
                "Chef’s Special Wrap",
                "Toasted wrap with sauce and crunch.",
                999,
                false
            ),

            MenuItem(
                id("starter_1"),
                restaurantId,
                "c_starters",
                "Crispy Fries",
                "Sea salt, served with dip.",
                399,
                true,
                addOnGroups = listOf(
                    AddOnGroup(
                        id = oid("starter_1", "ag_fries"),
                        title = "Extras",
                        required = false,
                        minSelections = 0,
                        maxSelections = 2,
                        options = listOf(
                            OptionItem(id = oid("starter_1", "extra_cheese"), title = "Cheese sauce", priceDeltaCents = 125),
                            OptionItem(id = oid("starter_1", "extra_spice"), title = "Spice seasoning", priceDeltaCents = 25)
                        )
                    )
                )
            ),
            MenuItem(
                id("starter_2"),
                restaurantId,
                "c_starters",
                "Spicy Wings",
                "Glazed, tangy, and hot.",
                699,
                false,
                variantGroups = listOf(wingsSpiceGroup),
                addOnGroups = listOf(wingsSauceAddOns)
            ),

            MenuItem(
                id("main_1"),
                restaurantId,
                "c_mains",
                "Classic Burger",
                "Juicy patty, lettuce, and house sauce.",
                1199,
                false,
                variantGroups = listOf(burgerCookGroup),
                addOnGroups = listOf(burgerAddOns)
            ),
            MenuItem(
                id("main_2"),
                restaurantId,
                "c_mains",
                "Paneer Tikka Plate",
                "Charred paneer with herbs and salad.",
                1299,
                true,
                variantGroups = listOf(
                    VariantGroup(
                        id = oid("main_2", "vg_spice"),
                        title = "Spice",
                        required = true,
                        options = listOf(
                            OptionItem(id = oid("main_2", "spice_mild"), title = "Mild", priceDeltaCents = 0),
                            OptionItem(id = oid("main_2", "spice_medium"), title = "Medium", priceDeltaCents = 0),
                            OptionItem(id = oid("main_2", "spice_hot"), title = "Hot", priceDeltaCents = 0)
                        )
                    )
                ),
                addOnGroups = listOf(
                    AddOnGroup(
                        id = oid("main_2", "ag_sides"),
                        title = "Add a side",
                        required = false,
                        minSelections = 0,
                        maxSelections = 2,
                        options = listOf(
                            OptionItem(id = oid("main_2", "side_naan"), title = "Butter naan", priceDeltaCents = 199),
                            OptionItem(id = oid("main_2", "side_raita"), title = "Raita", priceDeltaCents = 99)
                        )
                    )
                )
            ),

            MenuItem(id("dessert_1"), restaurantId, "c_desserts", "Chocolate Brownie", "Warm brownie with cocoa.", 499, true),
            MenuItem(id("dessert_2"), restaurantId, "c_desserts", "Cheesecake Slice", "Creamy and smooth.", 549, true),

            MenuItem(id("drink_1"), restaurantId, "c_drinks", "Iced Tea", "Freshly brewed, lightly sweet.", 249, true),
            MenuItem(id("drink_2"), restaurantId, "c_drinks", "Sparkling Water", "Chilled and crisp.", 199, true)
        )
    }

    fun restaurantById(id: String): Restaurant? = restaurants.firstOrNull { it.id == id }
}
