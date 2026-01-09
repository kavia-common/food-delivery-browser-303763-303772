package org.example.app.data.cart

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.example.app.data.models.CartLine
import org.example.app.data.models.ItemConfiguration
import org.example.app.data.models.MenuItem
import org.example.app.data.models.computeOptionsDeltaCents
import org.example.app.data.storage.PreferencesStorage
import org.example.app.data.storage.models.StoredCartLine
import kotlin.math.max

/**
 * In-memory cart repository (singleton) with local persistence.
 *
 * Keeps cart state during app runtime. Configuration changes are handled via ViewModel
 * observing LiveData exposed here.
 *
 * Call initialize(context) once on app start to hydrate from persisted state.
 */
object CartRepository {

    private var storage: PreferencesStorage? = null

    // Keyed by (itemId + configurationKey) to allow multiple distinct configurations per item.
    private val linesByKey: LinkedHashMap<String, CartLine> = LinkedHashMap()

    private val _cartLines = MutableLiveData<List<CartLine>>(emptyList())
    val cartLines: LiveData<List<CartLine>> = _cartLines

    private val _totalItemCount = MutableLiveData(0)
    val totalItemCount: LiveData<Int> = _totalItemCount

    // Derived pricing breakdown (subtotal/discount/fees/tax/total).
    private val _totals = MutableLiveData(
        CartTotals(
            itemCount = 0,
            subtotalCents = 0,
            discountCents = 0,
            deliveryFeeCents = 0,
            serviceFeeCents = 0,
            taxCents = 0,
            totalCents = 0
        )
    )
    val totals: LiveData<CartTotals> = _totals

    private val _appliedPromo = MutableLiveData<AppliedPromo?>(null)
    val appliedPromo: LiveData<AppliedPromo?> = _appliedPromo

    private val _feeSettings = MutableLiveData(
        FeeSettings(
            deliveryFeeCents = 199,
            serviceFeeCents = 99,
            taxRate = 0.08
        )
    )
    val feeSettings: LiveData<FeeSettings> = _feeSettings

    // PUBLIC_INTERFACE
    fun initialize(context: Context) {
        /** Initialize repo from SharedPreferences. Safe to call multiple times. */
        if (storage == null) storage = PreferencesStorage.from(context)
        val st = storage ?: return

        linesByKey.clear()
        st.loadCartLines().forEach { stored ->
            // Restore basic item fields. Note: mock data defines option groups; but for persistence we
            // reconstruct only the base item + persisted configuration ids.
            val item = MenuItem(
                id = stored.itemId,
                restaurantId = stored.restaurantId,
                categoryId = stored.categoryId,
                name = stored.name,
                description = stored.description,
                priceCents = stored.priceCents,
                isVeg = stored.isVeg
            )

            val configuration = ItemConfiguration(
                selectedVariantOptionIds = parseVariantSelectionMap(stored.selectedVariantOptionIds),
                selectedAddOnOptionIds = parseAddOnSelectionSet(stored.selectedAddOnOptionIds)
            )

            val key = buildLineKey(item.id, configuration)
            if (stored.quantity > 0) {
                linesByKey[key] = CartLine(item = item, configuration = configuration, quantity = stored.quantity)
            }
        }

        // Restore persisted pricing settings (promo + fees).
        _appliedPromo.value = st.loadCartPromo()

        val persistedFees = st.loadCartFeeSettings()
        if (persistedFees != null) {
            _feeSettings.value = persistedFees
        }

        // If cart is empty, promo should not be active (avoid confusing restore).
        if (linesByKey.isEmpty()) {
            _appliedPromo.value = null
        }

        publish(persist = false)
    }

    // PUBLIC_INTERFACE
    fun add(item: MenuItem) {
        /** Add one quantity for a menu item with default (first) required selections. */
        addConfigured(item, defaultConfigurationFor(item))
    }

    // PUBLIC_INTERFACE
    fun addConfigured(item: MenuItem, configuration: ItemConfiguration) {
        /** Add one quantity for a menu item with a specific configuration. */
        val key = buildLineKey(item.id, configuration)
        val existing = linesByKey[key]
        val newQ = (existing?.quantity ?: 0) + 1
        linesByKey[key] = CartLine(item = item, configuration = configuration, quantity = newQ)
        publish()
    }

    // PUBLIC_INTERFACE
    fun remove(line: CartLine) {
        /** Remove the line item entirely by configuration. */
        val key = buildLineKey(line.item.id, line.configuration)
        linesByKey.remove(key)
        publish()
    }

    // PUBLIC_INTERFACE
    fun remove(item: MenuItem) {
        /** Remove ALL lines for a menu item (all configurations). */
        val keysToRemove = linesByKey.keys.filter { it.startsWith(item.id + "#") }
        keysToRemove.forEach { linesByKey.remove(it) }
        publish()
    }

    // PUBLIC_INTERFACE
    fun updateQuantity(line: CartLine, quantity: Int) {
        /** Set line quantity (<=0 removes). Preserves configuration. */
        val q = max(0, quantity)
        val key = buildLineKey(line.item.id, line.configuration)
        if (q <= 0) linesByKey.remove(key) else linesByKey[key] = line.copy(quantity = q)
        publish()
    }

    // PUBLIC_INTERFACE
    fun updateQuantity(item: MenuItem, quantity: Int) {
        /**
         * Legacy API: if called, it updates the default configuration line only.
         * (Menu UI uses configuration picker when items have options.)
         */
        val conf = defaultConfigurationFor(item)
        val key = buildLineKey(item.id, conf)
        val q = max(0, quantity)
        if (q <= 0) linesByKey.remove(key) else linesByKey[key] = CartLine(item = item, configuration = conf, quantity = q)
        publish()
    }

    // PUBLIC_INTERFACE
    fun getQuantity(itemId: String): Int {
        /** Get total quantity across all configurations for an item ID. */
        return linesByKey.values.filter { it.item.id == itemId }.sumOf { it.quantity }
    }

    // PUBLIC_INTERFACE
    fun getLineQuantity(itemId: String, configuration: ItemConfiguration): Int {
        /** Get quantity for a specific (itemId + configuration). */
        val key = buildLineKey(itemId, configuration)
        return linesByKey[key]?.quantity ?: 0
    }

    // PUBLIC_INTERFACE
    fun getDefaultLine(itemId: String): CartLine? {
        /** Get the cart line for the default configuration (if any). */
        val key = buildLineKey(itemId, ItemConfiguration())
        return linesByKey[key]
    }

    // PUBLIC_INTERFACE
    fun applyPromoCode(rawCode: String): PromoApplyResult {
        /**
         * Apply a promo code (mock validation). Only one promo can be active.
         *
         * Edge cases:
         * - Empty cart: cannot apply.
         * - Blank code: invalid.
         * - Invalid/expired codes return an error result and do not change the active promo.
         */
        if (linesByKey.isEmpty()) return PromoApplyResult.CART_EMPTY

        val code = rawCode.trim().uppercase()
        if (code.isBlank()) return PromoApplyResult.INVALID

        val validated = validatePromoMock(code) ?: return PromoApplyResult.INVALID_OR_EXPIRED

        _appliedPromo.value = validated
        storage?.saveCartPromo(validated)
        publish(persist = true)
        return PromoApplyResult.APPLIED
    }

    // PUBLIC_INTERFACE
    fun removePromo() {
        /** Remove any active promo and persist. */
        _appliedPromo.value = null
        storage?.saveCartPromo(null)
        publish(persist = true)
    }

    // PUBLIC_INTERFACE
    fun setFeeSettings(settings: FeeSettings) {
        /**
         * Persist new fee settings and recompute totals.
         * This is a simple hook for future UI; current UI uses defaults.
         */
        _feeSettings.value = settings
        storage?.saveCartFeeSettings(settings)
        publish(persist = true)
    }

    // PUBLIC_INTERFACE
    fun computeSubtotalCents(): Int {
        /** Sum of (configured price * quantity). */
        return linesByKey.values.sumOf { line ->
            val unit = line.item.priceCents + computeOptionsDeltaCents(line.item, line.configuration)
            unit * line.quantity
        }
    }

    // PUBLIC_INTERFACE
    fun computeDiscountCents(): Int {
        /** Compute discount based on current promo and subtotal. Caps to subtotal. */
        val subtotal = computeSubtotalCents()
        val promo = _appliedPromo.value ?: return 0
        val discount = when (promo.kind) {
            PromoKind.PERCENT_OFF -> ((subtotal * promo.value) / 100.0).toInt()
            PromoKind.FIXED_CENTS_OFF -> promo.value
        }
        return discount.coerceIn(0, subtotal)
    }

    // PUBLIC_INTERFACE
    fun computeDeliveryFeeCents(): Int {
        /** Flat delivery fee; 0 if cart empty. */
        val fees = _feeSettings.value ?: defaultFeeSettings()
        return if (linesByKey.isEmpty()) 0 else fees.deliveryFeeCents
    }

    // PUBLIC_INTERFACE
    fun computeServiceFeeCents(): Int {
        /** Flat service fee; 0 if cart empty. */
        val fees = _feeSettings.value ?: defaultFeeSettings()
        return if (linesByKey.isEmpty()) 0 else fees.serviceFeeCents
    }

    // PUBLIC_INTERFACE
    fun computeTaxCents(): Int {
        /** Mock tax percent applied on (subtotal - discount) (i.e., taxed on discounted items). */
        if (linesByKey.isEmpty()) return 0
        val fees = _feeSettings.value ?: defaultFeeSettings()
        val taxableBase = (computeSubtotalCents() - computeDiscountCents()).coerceAtLeast(0)
        return (taxableBase * fees.taxRate).toInt()
    }

    // PUBLIC_INTERFACE
    fun computeTotalCents(): Int {
        /** Total = (subtotal - discount) + fees + tax. */
        return computeTotalsInternal().totalCents
    }

    private fun publish(persist: Boolean = true) {
        val list = linesByKey.values.toList()
        _cartLines.value = list
        _totalItemCount.value = list.sumOf { it.quantity }

        // If cart becomes empty, clear promo automatically (and persist the removal) to match expected UX.
        if (list.isEmpty() && _appliedPromo.value != null) {
            _appliedPromo.value = null
            storage?.saveCartPromo(null)
        }

        _totals.value = computeTotalsInternal()

        if (persist) persistCart(list)
    }

    private fun computeTotalsInternal(): CartTotals {
        val list = linesByKey.values.toList()
        val itemCount = list.sumOf { it.quantity }
        val subtotal = computeSubtotalCents()
        val discount = computeDiscountCents()
        val delivery = computeDeliveryFeeCents()
        val service = computeServiceFeeCents()
        val tax = computeTaxCents()
        val total = (subtotal - discount + delivery + service + tax).coerceAtLeast(0)

        return CartTotals(
            itemCount = itemCount,
            subtotalCents = subtotal,
            discountCents = discount,
            deliveryFeeCents = delivery,
            serviceFeeCents = service,
            taxCents = tax,
            totalCents = total
        )
    }

    private fun persistCart(lines: List<CartLine>) {
        // If initialize() wasn't called yet, skip persistence (no context available).
        val st = storage ?: return
        val storedLines = lines.map { line ->
            StoredCartLine(
                itemId = line.item.id,
                restaurantId = line.item.restaurantId,
                categoryId = line.item.categoryId,
                name = line.item.name,
                description = line.item.description,
                priceCents = line.item.priceCents,
                isVeg = line.item.isVeg,
                quantity = line.quantity,
                configurationKey = line.configuration.stableKey(),
                selectedVariantOptionIds = encodeVariantSelectionMap(line.configuration.selectedVariantOptionIds),
                selectedAddOnOptionIds = encodeAddOnSelectionSet(line.configuration.selectedAddOnOptionIds)
            )
        }
        st.saveCartLines(storedLines)
    }

    private fun defaultFeeSettings(): FeeSettings {
        return FeeSettings(
            deliveryFeeCents = 199,
            serviceFeeCents = 99,
            taxRate = 0.08
        )
    }

    /**
     * Simple mock validation:
     * - "SAVE10" => 10% off
     * - "SAVE5" => $5.00 off
     * - "FREESHIP" => $1.99 off (roughly covers default delivery fee)
     * - "EXPIRED" => invalid/expired
     */
    private fun validatePromoMock(code: String): AppliedPromo? {
        return when (code) {
            "SAVE10" -> AppliedPromo(code = code, kind = PromoKind.PERCENT_OFF, value = 10)
            "SAVE5" -> AppliedPromo(code = code, kind = PromoKind.FIXED_CENTS_OFF, value = 500)
            "FREESHIP" -> AppliedPromo(code = code, kind = PromoKind.FIXED_CENTS_OFF, value = 199)
            "EXPIRED" -> null
            else -> null
        }
    }

    private fun buildLineKey(itemId: String, configuration: ItemConfiguration): String {
        return "$itemId#${configuration.stableKey()}"
    }

    private fun encodeVariantSelectionMap(map: Map<String, String>): String {
        if (map.isEmpty()) return ""
        return map.toList()
            .sortedBy { it.first }
            .joinToString(separator = ",") { (g, o) -> "$g=$o" }
    }

    private fun parseVariantSelectionMap(encoded: String): Map<String, String> {
        if (encoded.isBlank()) return emptyMap()
        return encoded.split(",")
            .mapNotNull { token ->
                val trimmed = token.trim()
                if (trimmed.isBlank()) return@mapNotNull null
                val idx = trimmed.indexOf("=")
                if (idx <= 0 || idx >= trimmed.length - 1) return@mapNotNull null
                val g = trimmed.substring(0, idx)
                val o = trimmed.substring(idx + 1)
                if (g.isBlank() || o.isBlank()) null else g to o
            }
            .toMap()
    }

    private fun encodeAddOnSelectionSet(set: Set<String>): String {
        if (set.isEmpty()) return ""
        return set.toList().sorted().joinToString(separator = ",")
    }

    private fun parseAddOnSelectionSet(encoded: String): Set<String> {
        if (encoded.isBlank()) return emptySet()
        return encoded.split(",").mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } }.toSet()
    }

    private fun defaultConfigurationFor(item: MenuItem): ItemConfiguration {
        // Select the first option for required variant groups by default.
        val variantSelections = LinkedHashMap<String, String>()
        item.variantGroups.forEach { group ->
            if (group.required) {
                val first = group.options.firstOrNull()
                if (first != null) variantSelections[group.id] = first.id
            }
        }
        return ItemConfiguration(
            selectedVariantOptionIds = variantSelections,
            selectedAddOnOptionIds = emptySet()
        )
    }
}

/**
 * Result for applying a promo code (used by UI to show messages).
 */
enum class PromoApplyResult {
    APPLIED,
    CART_EMPTY,
    INVALID,
    INVALID_OR_EXPIRED
}
