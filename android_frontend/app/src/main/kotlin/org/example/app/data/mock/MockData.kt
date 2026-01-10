package org.example.app.data.mock

import org.example.app.data.models.AddOnGroup
import org.example.app.data.models.MenuCategory
import org.example.app.data.models.MenuItem
import org.example.app.data.models.OptionItem
import org.example.app.data.models.Restaurant
import org.example.app.data.models.VariantGroup

object MockData {

    // NOTE: This list is intentionally sized to keep the demo snappy while still varied.
    // Avoid adding huge payloads here because menus are computed on demand in Home sorting.
    val restaurants: List<Restaurant> = listOf(
        Restaurant("r1", "Blue Bowl Kitchen", listOf("Healthy", "Bowls", "Salads"), 4.6, 25, 35, "#DBEAFE", imageUrl = null),
        Restaurant("r2", "Spice Route", listOf("Indian", "Curry", "Biryani"), 4.3, 30, 45, "#DCFCE7", imageUrl = null),
        Restaurant("r3", "Sushi Station", listOf("Japanese", "Sushi", "Bento"), 4.5, 20, 35, "#FFE4E6", imageUrl = null),
        Restaurant("r4", "Pasta & Co.", listOf("Italian", "Pasta", "Pizza"), 4.2, 25, 40, "#FEF3C7", imageUrl = null),
        Restaurant("r5", "Taco Town", listOf("Mexican", "Tacos", "Burritos"), 4.4, 20, 30, "#E0F2FE", imageUrl = null),
        Restaurant("r6", "Burger Bureau", listOf("Burgers", "Fries", "Shakes"), 4.1, 20, 35, "#F3E8FF", imageUrl = null),
        Restaurant("r7", "Green Garden", listOf("Vegetarian", "Vegan", "Fresh"), 4.7, 25, 40, "#ECFCCB", imageUrl = null),
        Restaurant("r8", "Sweet Studio", listOf("Desserts", "Cakes", "Ice Cream"), 4.5, 25, 40, "#FFE4E6", imageUrl = null),
        Restaurant("r9", "Wok Waves", listOf("Chinese", "Noodles", "Stir-fry"), 4.2, 25, 40, "#E5E7EB", imageUrl = null),
        Restaurant("r10", "Mediterraneo", listOf("Mediterranean", "Kebab", "Bowls"), 4.4, 25, 40, "#DBEAFE", imageUrl = null),

        // Additional restaurants (10–15) with diverse cuisines/price vibes and veg/non-veg coverage
        Restaurant("r11", "Seoul Street", listOf("Korean", "BBQ", "Rice Bowls"), 4.5, 25, 40, "#FEE2E2", imageUrl = null),
        Restaurant("r12", "Pho & Roll", listOf("Vietnamese", "Pho", "Banh Mi"), 4.4, 20, 35, "#DCFCE7", imageUrl = null),
        Restaurant("r13", "Falafel Factory", listOf("Mediterranean", "Vegetarian", "Wraps"), 4.6, 20, 35, "#ECFCCB", imageUrl = null),
        Restaurant("r14", "Breakfast Club", listOf("Breakfast", "Cafe", "Sandwiches"), 4.3, 15, 30, "#E0E7FF", imageUrl = null),
        Restaurant("r15", "BBQ Barn", listOf("BBQ", "American", "Smoked"), 4.2, 35, 55, "#FEF3C7", imageUrl = null),
        Restaurant("r16", "Coastal Catch", listOf("Seafood", "Grill", "Salads"), 4.4, 30, 50, "#E0F2FE", imageUrl = null),
        Restaurant("r17", "Tapas & Tiles", listOf("Spanish", "Tapas", "Small Plates"), 4.3, 25, 45, "#F3E8FF", imageUrl = null),
        Restaurant("r18", "Little Lebanon", listOf("Middle Eastern", "Kebab", "Mezze"), 4.5, 25, 45, "#DBEAFE", imageUrl = null),
        Restaurant("r19", "Ramen Alley", listOf("Japanese", "Ramen", "Noodles"), 4.6, 20, 35, "#FFE4E6", imageUrl = null),
        Restaurant("r20", "Bistro Bakes", listOf("Bakery", "Desserts", "Cafe"), 4.2, 20, 35, "#F1F5F9", imageUrl = null)
    )

    val categories: List<MenuCategory> = listOf(
        MenuCategory("c_popular", "Popular"),
        MenuCategory("c_starters", "Starters"),
        MenuCategory("c_mains", "Mains"),
        MenuCategory("c_combos", "Combos"),
        MenuCategory("c_desserts", "Desserts"),
        MenuCategory("c_drinks", "Drinks")
    )

    fun menuForRestaurant(restaurantId: String): List<MenuItem> {
        fun id(suffix: String) = "${restaurantId}_$suffix"
        fun oid(itemSuffix: String, optSuffix: String) = "${id(itemSuffix)}_$optSuffix"

        // Reusable option groups (IDs are per-restaurant to keep them unique and stable)
        val sizeGroup = VariantGroup(
            id = oid("vg_common", "vg_size"),
            title = "Size",
            required = true,
            options = listOf(
                OptionItem(id = oid("vg_common", "size_regular"), title = "Regular", priceDeltaCents = 0),
                OptionItem(id = oid("vg_common", "size_large"), title = "Large", priceDeltaCents = 250)
            )
        )

        val spiceGroup = VariantGroup(
            id = oid("vg_common", "vg_spice"),
            title = "Spice",
            required = true,
            options = listOf(
                OptionItem(id = oid("vg_common", "spice_mild"), title = "Mild", priceDeltaCents = 0),
                OptionItem(id = oid("vg_common", "spice_medium"), title = "Medium", priceDeltaCents = 0),
                OptionItem(id = oid("vg_common", "spice_hot"), title = "Hot", priceDeltaCents = 0),
                OptionItem(id = oid("vg_common", "spice_extra_hot"), title = "Extra hot", priceDeltaCents = 50)
            )
        )

        val dipAddOns = AddOnGroup(
            id = oid("ag_common", "ag_dips"),
            title = "Dips",
            required = false,
            minSelections = 0,
            maxSelections = 2,
            options = listOf(
                OptionItem(id = oid("ag_common", "dip_ranch"), title = "Ranch dip", priceDeltaCents = 75),
                OptionItem(id = oid("ag_common", "dip_bbq"), title = "BBQ dip", priceDeltaCents = 75),
                OptionItem(id = oid("ag_common", "dip_garlic"), title = "Garlic mayo", priceDeltaCents = 75)
            )
        )

        val toppingsAddOns = AddOnGroup(
            id = oid("ag_common", "ag_toppings"),
            title = "Toppings",
            required = false,
            minSelections = 0,
            maxSelections = 4,
            options = listOf(
                OptionItem(id = oid("ag_common", "top_cheese"), title = "Cheese", priceDeltaCents = 100),
                OptionItem(id = oid("ag_common", "top_avocado"), title = "Avocado", priceDeltaCents = 150),
                OptionItem(id = oid("ag_common", "top_bacon"), title = "Bacon", priceDeltaCents = 200),
                OptionItem(id = oid("ag_common", "top_jalapeno"), title = "Jalapeño", priceDeltaCents = 50)
            )
        )

        // Keep per-restaurant list moderate (about ~18 items) for performance.
        return listOf(
            // Popular
            MenuItem(
                id = id("popular_1"),
                restaurantId = restaurantId,
                categoryId = "c_popular",
                name = "House Signature",
                description = "The dish we’re known for—balanced, comforting, and craveable.",
                priceCents = when (restaurantId) {
                    "r8", "r20" -> 599
                    "r14" -> 899
                    else -> 1099
                },
                isVeg = restaurantId in setOf("r1", "r7", "r13", "r20"),
                variantGroups = listOf(sizeGroup),
                addOnGroups = listOf(toppingsAddOns),
                imageUrl = null
            ),
            MenuItem(
                id = id("popular_2"),
                restaurantId = restaurantId,
                categoryId = "c_popular",
                name = "Chef’s Special Wrap",
                description = "Toasted wrap with sauce and crunch.",
                priceCents = 999,
                isVeg = restaurantId in setOf("r1", "r7", "r13", "r14", "r20"),
                imageUrl = null
            ),
            MenuItem(
                id = id("popular_3"),
                restaurantId = restaurantId,
                categoryId = "c_popular",
                name = "Crispy Fries",
                description = "Sea salt, served with dip.",
                priceCents = 399,
                isVeg = true,
                addOnGroups = listOf(dipAddOns),
                imageUrl = null
            ),

            // Starters
            MenuItem(
                id = id("starter_1"),
                restaurantId = restaurantId,
                categoryId = "c_starters",
                name = "Garlic Bread Bites",
                description = "Buttery, toasted bites with herbs.",
                priceCents = 449,
                isVeg = true,
                addOnGroups = listOf(
                    AddOnGroup(
                        id = oid("starter_1", "ag_extra"),
                        title = "Extras",
                        required = false,
                        minSelections = 0,
                        maxSelections = 2,
                        options = listOf(
                            OptionItem(id = oid("starter_1", "extra_cheese"), title = "Extra cheese", priceDeltaCents = 125),
                            OptionItem(id = oid("starter_1", "extra_chili_flakes"), title = "Chili flakes", priceDeltaCents = 25)
                        )
                    )
                ),
                imageUrl = null
            ),
            MenuItem(
                id = id("starter_2"),
                restaurantId = restaurantId,
                categoryId = "c_starters",
                name = "Spicy Wings",
                description = "Glazed, tangy, and hot.",
                priceCents = 699,
                isVeg = false,
                variantGroups = listOf(spiceGroup),
                addOnGroups = listOf(dipAddOns),
                imageUrl = null
            ),
            MenuItem(
                id = id("starter_3"),
                restaurantId = restaurantId,
                categoryId = "c_starters",
                name = "Edamame",
                description = "Steamed with sea salt.",
                priceCents = 399,
                isVeg = true,
                imageUrl = null
            ),

            // Mains
            MenuItem(
                id = id("main_1"),
                restaurantId = restaurantId,
                categoryId = "c_mains",
                name = "Classic Burger",
                description = "Juicy patty, lettuce, and house sauce.",
                priceCents = 1199,
                isVeg = false,
                variantGroups = listOf(
                    VariantGroup(
                        id = oid("main_1", "vg_cook"),
                        title = "Cook",
                        required = true,
                        options = listOf(
                            OptionItem(id = oid("main_1", "cook_medium"), title = "Medium", priceDeltaCents = 0),
                            OptionItem(id = oid("main_1", "cook_well"), title = "Well done", priceDeltaCents = 0)
                        )
                    )
                ),
                addOnGroups = listOf(toppingsAddOns),
                imageUrl = null
            ),
            MenuItem(
                id = id("main_2"),
                restaurantId = restaurantId,
                categoryId = "c_mains",
                name = "Paneer Tikka Plate",
                description = "Charred paneer with herbs and salad.",
                priceCents = 1299,
                isVeg = true,
                variantGroups = listOf(spiceGroup),
                addOnGroups = listOf(
                    AddOnGroup(
                        id = oid("main_2", "ag_sides"),
                        title = "Add a side",
                        required = false,
                        minSelections = 0,
                        maxSelections = 2,
                        options = listOf(
                            OptionItem(id = oid("main_2", "side_naan"), title = "Butter naan", priceDeltaCents = 199),
                            OptionItem(id = oid("main_2", "side_raita"), title = "Raita", priceDeltaCents = 99),
                            OptionItem(id = oid("main_2", "side_salad"), title = "Cucumber salad", priceDeltaCents = 125)
                        )
                    )
                ),
                imageUrl = null
            ),
            MenuItem(
                id = id("main_3"),
                restaurantId = restaurantId,
                categoryId = "c_mains",
                name = "Teriyaki Chicken Bowl",
                description = "Sweet-savory glaze, rice, and crunchy veg.",
                priceCents = 1249,
                isVeg = false,
                variantGroups = listOf(sizeGroup),
                addOnGroups = listOf(
                    AddOnGroup(
                        id = oid("main_3", "ag_protein"),
                        title = "Protein add-ons",
                        required = false,
                        minSelections = 0,
                        maxSelections = 2,
                        options = listOf(
                            OptionItem(id = oid("main_3", "extra_chicken"), title = "Extra chicken", priceDeltaCents = 250),
                            OptionItem(id = oid("main_3", "extra_egg"), title = "Add egg", priceDeltaCents = 100)
                        )
                    )
                ),
                imageUrl = null
            ),
            MenuItem(
                id = id("main_4"),
                restaurantId = restaurantId,
                categoryId = "c_mains",
                name = "Veggie Ramen",
                description = "Brothy noodles with mushrooms and greens.",
                priceCents = 1199,
                isVeg = true,
                variantGroups = listOf(
                    VariantGroup(
                        id = oid("main_4", "vg_noodle"),
                        title = "Noodles",
                        required = true,
                        options = listOf(
                            OptionItem(id = oid("main_4", "noodle_wheat"), title = "Wheat noodles", priceDeltaCents = 0),
                            OptionItem(id = oid("main_4", "noodle_rice"), title = "Rice noodles", priceDeltaCents = 0)
                        )
                    )
                ),
                addOnGroups = listOf(
                    AddOnGroup(
                        id = oid("main_4", "ag_addons"),
                        title = "Add-ons",
                        required = false,
                        minSelections = 0,
                        maxSelections = 3,
                        options = listOf(
                            OptionItem(id = oid("main_4", "addon_chili_oil"), title = "Chili oil", priceDeltaCents = 25),
                            OptionItem(id = oid("main_4", "addon_toast_nori"), title = "Nori", priceDeltaCents = 75),
                            OptionItem(id = oid("main_4", "addon_soft_egg"), title = "Soft egg", priceDeltaCents = 125)
                        )
                    )
                ),
                imageUrl = null
            ),

            // Combos
            MenuItem(
                id = id("combo_1"),
                restaurantId = restaurantId,
                categoryId = "c_combos",
                name = "Lunch Combo",
                description = "Main + side + drink (best value).",
                priceCents = 1499,
                isVeg = restaurantId in setOf("r1", "r7", "r13", "r14", "r20"),
                variantGroups = listOf(
                    VariantGroup(
                        id = oid("combo_1", "vg_drink"),
                        title = "Drink",
                        required = true,
                        options = listOf(
                            OptionItem(id = oid("combo_1", "drink_iced_tea"), title = "Iced tea", priceDeltaCents = 0),
                            OptionItem(id = oid("combo_1", "drink_soda"), title = "Soda", priceDeltaCents = 0),
                            OptionItem(id = oid("combo_1", "drink_sparkling"), title = "Sparkling water", priceDeltaCents = 50)
                        )
                    )
                ),
                addOnGroups = listOf(
                    AddOnGroup(
                        id = oid("combo_1", "ag_upgrade"),
                        title = "Upgrade",
                        required = false,
                        minSelections = 0,
                        maxSelections = 1,
                        options = listOf(
                            OptionItem(id = oid("combo_1", "upgrade_dessert"), title = "Add a dessert", priceDeltaCents = 249),
                            OptionItem(id = oid("combo_1", "upgrade_side"), title = "Add extra side", priceDeltaCents = 199)
                        )
                    )
                ),
                imageUrl = null
            ),
            MenuItem(
                id = id("combo_2"),
                restaurantId = restaurantId,
                categoryId = "c_combos",
                name = "Family Feast",
                description = "2 mains + 2 sides—made for sharing.",
                priceCents = 2899,
                isVeg = restaurantId in setOf("r7", "r13"),
                imageUrl = null
            ),

            // Desserts
            MenuItem(
                id = id("dessert_1"),
                restaurantId = restaurantId,
                categoryId = "c_desserts",
                name = "Chocolate Brownie",
                description = "Warm brownie with cocoa.",
                priceCents = 499,
                isVeg = true,
                imageUrl = null
            ),
            MenuItem(
                id = id("dessert_2"),
                restaurantId = restaurantId,
                categoryId = "c_desserts",
                name = "Cheesecake Slice",
                description = "Creamy and smooth.",
                priceCents = 549,
                isVeg = true,
                imageUrl = null
            ),
            MenuItem(
                id = id("dessert_3"),
                restaurantId = restaurantId,
                categoryId = "c_desserts",
                name = "Seasonal Fruit Cup",
                description = "Chilled fruit with mint.",
                priceCents = 399,
                isVeg = true,
                imageUrl = null
            ),

            // Drinks
            MenuItem(
                id = id("drink_1"),
                restaurantId = restaurantId,
                categoryId = "c_drinks",
                name = "Iced Tea",
                description = "Freshly brewed, lightly sweet.",
                priceCents = 249,
                isVeg = true,
                imageUrl = null
            ),
            MenuItem(
                id = id("drink_2"),
                restaurantId = restaurantId,
                categoryId = "c_drinks",
                name = "Sparkling Water",
                description = "Chilled and crisp.",
                priceCents = 199,
                isVeg = true,
                imageUrl = null
            ),
            MenuItem(
                id = id("drink_3"),
                restaurantId = restaurantId,
                categoryId = "c_drinks",
                name = "Mango Lassi",
                description = "Yogurt drink with mango.",
                priceCents = 349,
                isVeg = true,
                imageUrl = null
            )
        )
    }

    fun restaurantById(id: String): Restaurant? = restaurants.firstOrNull { it.id == id }
}
