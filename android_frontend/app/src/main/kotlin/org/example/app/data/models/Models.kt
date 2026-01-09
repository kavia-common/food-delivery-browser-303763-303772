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
 * A single selectable option belonging to either a [VariantGroup] or [AddOnGroup].
 *
 * @param priceDeltaCents The incremental cents to add to the base price when selected.
 */
data class OptionItem(
    val id: String,
    val title: String,
    val priceDeltaCents: Int
)

/**
 * Variants are single-select groups (e.g., Size, Spice level).
 *
 * @param required Whether a selection is required.
 */
data class VariantGroup(
    val id: String,
    val title: String,
    val required: Boolean,
    val options: List<OptionItem>
)

/**
 * Add-ons are multi-select groups (e.g., Extra cheese, Add a drink).
 *
 * @param required Whether the group requires selecting at least [minSelections].
 * @param minSelections Minimum selections required (0 allowed when not required).
 * @param maxSelections Maximum selections allowed (null means unlimited).
 */
data class AddOnGroup(
    val id: String,
    val title: String,
    val required: Boolean,
    val minSelections: Int = 0,
    val maxSelections: Int? = null,
    val options: List<OptionItem>
)

/**
 * Represents a menu item user can add to cart.
 *
 * @param variantGroups Single-select option groups.
 * @param addOnGroups Multi-select option groups.
 */
data class MenuItem(
    val id: String,
    val restaurantId: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val priceCents: Int,
    val isVeg: Boolean,
    val variantGroups: List<VariantGroup> = emptyList(),
    val addOnGroups: List<AddOnGroup> = emptyList()
)

/**
 * The selected configuration for a cart line.
 *
 * @param selectedVariantOptionIds Map: variantGroupId -> optionId.
 * @param selectedAddOnOptionIds Set of add-on option ids (across all groups).
 */
data class ItemConfiguration(
    val selectedVariantOptionIds: Map<String, String> = emptyMap(),
    val selectedAddOnOptionIds: Set<String> = emptySet()
) {
    /**
     * Stable key used for distinguishing unique configurations of the same [MenuItem].
     * This key is intended for internal cart indexing and persistence.
     */
    fun stableKey(): String {
        val variants = selectedVariantOptionIds.toList().sortedBy { it.first }
            .joinToString(separator = ",") { (g, o) -> "$g=$o" }
        val addons = selectedAddOnOptionIds.toList().sorted()
            .joinToString(separator = ",")
        return "v{$variants}|a{$addons}"
    }
}

/**
 * Cart line item (menu item + configuration + quantity).
 *
 * Distinct configurations of the same menu item must result in separate lines.
 */
data class CartLine(
    val item: MenuItem,
    val configuration: ItemConfiguration,
    val quantity: Int,
    val itemNote: String = ""
)

/**
 * PUBLIC_INTERFACE
 * Compute total option delta (variants + add-ons) for a given menu item + configuration.
 */
fun computeOptionsDeltaCents(item: MenuItem, configuration: ItemConfiguration): Int {
    val variantDelta = item.variantGroups.sumOf { group ->
        val selectedOptionId = configuration.selectedVariantOptionIds[group.id] ?: return@sumOf 0
        group.options.firstOrNull { it.id == selectedOptionId }?.priceDeltaCents ?: 0
    }

    val addOnDelta = item.addOnGroups.sumOf { group ->
        group.options.filter { configuration.selectedAddOnOptionIds.contains(it.id) }
            .sumOf { it.priceDeltaCents }
    }

    return variantDelta + addOnDelta
}

/**
 * PUBLIC_INTERFACE
 * Human-readable summary of chosen options for cart UI.
 */
fun buildConfigurationSummary(item: MenuItem, configuration: ItemConfiguration): String {
    val parts = ArrayList<String>()

    item.variantGroups.forEach { group ->
        val optionId = configuration.selectedVariantOptionIds[group.id] ?: return@forEach
        val optionTitle = group.options.firstOrNull { it.id == optionId }?.title ?: return@forEach
        parts.add("${group.title}: $optionTitle")
    }

    val addOns = ArrayList<String>()
    item.addOnGroups.forEach { group ->
        group.options.forEach { opt ->
            if (configuration.selectedAddOnOptionIds.contains(opt.id)) {
                addOns.add(opt.title)
            }
        }
    }
    if (addOns.isNotEmpty()) {
        parts.add("Add-ons: " + addOns.joinToString(", "))
    }

    return parts.joinToString(" • ")
}
