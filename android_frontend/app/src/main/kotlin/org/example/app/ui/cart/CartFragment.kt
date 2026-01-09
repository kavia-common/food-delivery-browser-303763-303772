package org.example.app.ui.cart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.example.app.MainActivity
import org.example.app.R
import org.example.app.common.Formatters
import org.example.app.data.cart.CartRepository
import org.example.app.data.cart.CartTotals
import org.example.app.data.cart.PromoApplyResult
import org.example.app.data.delivery.DeliveryRepository
import org.example.app.data.mock.MockData
import org.example.app.data.models.CartLine
import org.example.app.ui.menu.ItemOptionsBottomSheet

class CartFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CartAdapter

    private lateinit var emptyState: View
    private lateinit var totalsContainer: View

    // Promo UI
    private lateinit var promoInputLayout: TextInputLayout
    private lateinit var promoEditText: TextInputEditText
    private lateinit var applyPromoButton: MaterialButton
    private lateinit var removePromoButton: MaterialButton
    private lateinit var promoAppliedHint: TextView

    // Order instructions UI
    private lateinit var orderInstructionsLayout: TextInputLayout
    private lateinit var orderInstructionsEditText: TextInputEditText

    // Breakdown UI
    private lateinit var subtotalValue: TextView
    private lateinit var discountRow: View
    private lateinit var discountLabel: TextView
    private lateinit var discountValue: TextView
    private lateinit var deliveryValue: TextView
    private lateinit var serviceFeeValue: TextView
    private lateinit var taxValue: TextView
    private lateinit var totalValue: TextView

    private lateinit var placeOrderButton: MaterialButton
    private lateinit var checkoutButton: MaterialButton
    private lateinit var checkoutHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = CartAdapter(
            onInc = { line -> CartRepository.updateQuantity(line, line.quantity + 1) },
            onDec = { line -> CartRepository.updateQuantity(line, line.quantity - 1) },
            onRemove = { line -> CartRepository.remove(line) },
            onEdit = { line -> openEditOptions(line) }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.cartRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        emptyState = view.findViewById(R.id.emptyState)
        totalsContainer = view.findViewById(R.id.totalsContainer)

        promoInputLayout = view.findViewById(R.id.promoInputLayout)
        promoEditText = view.findViewById(R.id.promoEditText)
        applyPromoButton = view.findViewById(R.id.applyPromoButton)
        removePromoButton = view.findViewById(R.id.removePromoButton)
        promoAppliedHint = view.findViewById(R.id.promoAppliedHint)

        orderInstructionsLayout = view.findViewById(R.id.orderInstructionsLayout)
        orderInstructionsEditText = view.findViewById(R.id.orderInstructionsEditText)

        subtotalValue = view.findViewById(R.id.subtotalValue)
        discountRow = view.findViewById(R.id.discountRow)
        discountLabel = view.findViewById(R.id.discountLabel)
        discountValue = view.findViewById(R.id.discountValue)
        deliveryValue = view.findViewById(R.id.deliveryValue)
        serviceFeeValue = view.findViewById(R.id.serviceFeeValue)
        taxValue = view.findViewById(R.id.taxValue)
        totalValue = view.findViewById(R.id.totalValue)

        placeOrderButton = view.findViewById(R.id.placeOrderButton)
        checkoutButton = view.findViewById(R.id.checkoutButton)
        checkoutHint = view.findViewById(R.id.checkoutHint)

        checkoutButton.isEnabled = false
        checkoutHint.isVisible = true

        placeOrderButton.setOnClickListener {
            val lines = CartRepository.cartLines.value ?: emptyList()
            if (lines.isEmpty()) return@setOnClickListener

            // Pick restaurant name from mock data; fallback to first line's restaurant id.
            val restaurantId = lines.first().item.restaurantId
            val restaurantName = MockData.restaurants.firstOrNull { it.id == restaurantId }?.name ?: "Restaurant"

            // Ensure latest instructions are persisted before placing.
            CartRepository.setOrderInstructions(orderInstructionsEditText.text?.toString().orEmpty())

            val created = DeliveryRepository.placeOrderFromCart(requireContext(), restaurantName)
            if (created != null) {
                // Clear cart after placing order to mimic real UX.
                lines.forEach { CartRepository.remove(it) }

                // Clear per-order instructions only after a successful order placement.
                CartRepository.clearOrderInstructions()

                // Navigate to Delivery tab/screen.
                (activity as? MainActivity)?.openDelivery()
            }
        }

        applyPromoButton.setOnClickListener {
            // Clear any previous error and attempt apply.
            promoInputLayout.error = null
            val result = CartRepository.applyPromoCode(promoEditText.text?.toString().orEmpty())
            when (result) {
                PromoApplyResult.APPLIED -> {
                    promoInputLayout.error = null
                }

                PromoApplyResult.CART_EMPTY -> {
                    promoInputLayout.error = getString(R.string.promo_cart_empty)
                }

                PromoApplyResult.INVALID -> {
                    promoInputLayout.error = getString(R.string.promo_invalid)
                }

                PromoApplyResult.INVALID_OR_EXPIRED -> {
                    promoInputLayout.error = getString(R.string.promo_invalid_or_expired)
                }
            }
        }

        removePromoButton.setOnClickListener {
            promoInputLayout.error = null
            promoEditText.setText("")
            CartRepository.removePromo()
        }

        // Persist instructions when user finishes editing.
        orderInstructionsEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val raw = orderInstructionsEditText.text?.toString().orEmpty()
                // Keep UI error-free; counter enforces length; repository trims.
                orderInstructionsLayout.error = null
                CartRepository.setOrderInstructions(raw)
            }
        }

        // Observe changes and render.
        CartRepository.cartLines.observe(viewLifecycleOwner) { lines ->
            adapter.submitList(lines)
            val isEmpty = lines.isEmpty()
            emptyState.isVisible = isEmpty
            totalsContainer.isVisible = !isEmpty

            placeOrderButton.isEnabled = !isEmpty

            // Edge case: empty cart disables promo apply.
            applyPromoButton.isEnabled = !isEmpty
            removePromoButton.isEnabled = !isEmpty

            // Disable instructions input when cart is empty (matches “per-order” behavior).
            orderInstructionsLayout.isEnabled = !isEmpty
        }

        CartRepository.orderInstructions.observe(viewLifecycleOwner) { instructions ->
            val current = orderInstructionsEditText.text?.toString().orEmpty()
            if (current != instructions) {
                orderInstructionsEditText.setText(instructions)
                orderInstructionsEditText.setSelection(instructions.length)
            }
        }

        CartRepository.appliedPromo.observe(viewLifecycleOwner) { promo ->
            val hasPromo = promo != null
            promoAppliedHint.isVisible = hasPromo
            promoAppliedHint.text = if (promo != null) {
                getString(R.string.promo_applied, promo.code)
            } else {
                ""
            }
            // When promo is active, keep the input populated with the code for clarity.
            if (promo != null && promoEditText.text?.toString()?.trim()?.equals(promo.code, ignoreCase = true) != true) {
                promoEditText.setText(promo.code)
                promoEditText.setSelection(promo.code.length)
            }
        }

        CartRepository.totals.observe(viewLifecycleOwner) { totals ->
            renderTotals(totals)
        }
    }

    private fun openEditOptions(line: CartLine) {
        val item = line.item

        // If item has no option metadata (e.g., restored from persistence without groups),
        // editing is not meaningful. Keep it safe by no-op.
        val hasOptions = item.variantGroups.isNotEmpty() || item.addOnGroups.isNotEmpty()
        if (!hasOptions) return

        val sheet = ItemOptionsBottomSheet.newInstance()
        sheet.bind(
            item = item,
            initialConfiguration = line.configuration,
            initialQuantity = line.quantity,
            isEditMode = true,
            initialItemNote = line.itemNote,
            listener = object : ItemOptionsBottomSheet.Listener {
                override fun onConfirmed(
                    item: org.example.app.data.models.MenuItem,
                    configuration: org.example.app.data.models.ItemConfiguration,
                    quantity: Int,
                    itemNote: String
                ) {
                    // Replace old line with new configuration preserving quantity.
                    CartRepository.remove(line)
                    CartRepository.addConfigured(item, configuration)

                    val newLineQty = CartRepository.getLineQuantity(item.id, configuration)

                    // addConfigured adds 1; adjust to desired quantity
                    if (quantity != newLineQty) {
                        CartRepository.updateQuantity(
                            org.example.app.data.models.CartLine(item, configuration, newLineQty),
                            quantity
                        )
                    }

                    // Persist note on the resulting line.
                    val finalQty = CartRepository.getLineQuantity(item.id, configuration)
                    if (finalQty > 0) {
                        CartRepository.updateLineItemNote(
                            org.example.app.data.models.CartLine(item, configuration, finalQty),
                            itemNote
                        )
                    }
                }
            }
        )
        sheet.show(parentFragmentManager, "EditItemOptionsBottomSheet")
    }

    private fun renderTotals(totals: CartTotals) {
        subtotalValue.text = Formatters.moneyFromCents(totals.subtotalCents)

        val hasDiscount = totals.discountCents > 0
        discountRow.isVisible = hasDiscount
        if (hasDiscount) {
            val promo = CartRepository.appliedPromo.value
            discountLabel.text = if (promo != null) {
                getString(R.string.discount_with_code, promo.code)
            } else {
                getString(R.string.discount)
            }
            // Show discount as negative.
            discountValue.text = "-" + Formatters.moneyFromCents(totals.discountCents)
        }

        deliveryValue.text = Formatters.moneyFromCents(totals.deliveryFeeCents)
        serviceFeeValue.text = Formatters.moneyFromCents(totals.serviceFeeCents)
        taxValue.text = Formatters.moneyFromCents(totals.taxCents)
        totalValue.text = Formatters.moneyFromCents(totals.totalCents)
    }

    companion object {
        const val TAG = "CartFragment"

        fun newInstance(): CartFragment = CartFragment()
    }
}
