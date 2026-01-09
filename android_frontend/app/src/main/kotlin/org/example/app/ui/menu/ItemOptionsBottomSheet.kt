package org.example.app.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import org.example.app.R
import org.example.app.common.Formatters
import org.example.app.data.models.AddOnGroup
import org.example.app.data.models.ItemConfiguration
import org.example.app.data.models.MenuItem
import org.example.app.data.models.VariantGroup
import org.example.app.data.models.computeOptionsDeltaCents

/**
 * Bottom sheet used to select variants (single-select) and add-ons (multi-select) before adding/editing a cart line.
 */
class ItemOptionsBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun onConfirmed(item: MenuItem, configuration: ItemConfiguration, quantity: Int, itemNote: String)
    }

    private var listener: Listener? = null
    private lateinit var item: MenuItem

    private var initialConfiguration: ItemConfiguration = ItemConfiguration()
    private var initialQuantity: Int = 1
    private var isEditMode: Boolean = false
    private var initialItemNote: String = ""

    private val selectedVariantOptionIds: MutableMap<String, String> = LinkedHashMap()
    private val selectedAddOnOptionIds: MutableSet<String> = LinkedHashSet()

    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var optionsContainer: LinearLayout
    private lateinit var priceText: TextView
    private lateinit var errorText: TextView
    private lateinit var cancelButton: MaterialButton
    private lateinit var confirmButton: MaterialButton

    private lateinit var itemNoteInputLayout: com.google.android.material.textfield.TextInputLayout
    private lateinit var itemNoteEditText: com.google.android.material.textfield.TextInputEditText

    // PUBLIC_INTERFACE
    fun bind(
        item: MenuItem,
        initialConfiguration: ItemConfiguration,
        initialQuantity: Int,
        isEditMode: Boolean,
        initialItemNote: String,
        listener: Listener
    ) {
        /** Bind sheet with the menu item and initial selections. Call before show(). */
        this.item = item
        this.initialConfiguration = initialConfiguration
        this.initialQuantity = initialQuantity
        this.isEditMode = isEditMode
        this.initialItemNote = initialItemNote
        this.listener = listener

        selectedVariantOptionIds.clear()
        selectedVariantOptionIds.putAll(initialConfiguration.selectedVariantOptionIds)
        selectedAddOnOptionIds.clear()
        selectedAddOnOptionIds.addAll(initialConfiguration.selectedAddOnOptionIds)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.sheet_item_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        title = view.findViewById(R.id.optionsTitle)
        subtitle = view.findViewById(R.id.optionsSubtitle)
        optionsContainer = view.findViewById(R.id.optionsContainer)
        priceText = view.findViewById(R.id.optionsPrice)
        errorText = view.findViewById(R.id.optionsError)
        cancelButton = view.findViewById(R.id.optionsCancel)
        confirmButton = view.findViewById(R.id.optionsConfirm)

        itemNoteInputLayout = view.findViewById(R.id.itemNoteInputLayout)
        itemNoteEditText = view.findViewById(R.id.itemNoteEditText)

        title.text = item.name
        subtitle.text = item.description
        subtitle.isVisible = item.description.isNotBlank()

        itemNoteEditText.setText(initialItemNote)
        itemNoteEditText.setSelection(itemNoteEditText.text?.length ?: 0)

        optionsContainer.removeAllViews()

        item.variantGroups.forEach { group ->
            optionsContainer.addView(buildVariantGroupView(group))
        }
        item.addOnGroups.forEach { group ->
            optionsContainer.addView(buildAddOnGroupView(group))
        }

        cancelButton.setOnClickListener { dismissAllowingStateLoss() }
        confirmButton.text = if (isEditMode) getString(R.string.save) else getString(R.string.add_to_cart)
        confirmButton.setOnClickListener { onConfirm() }

        renderPrice()
    }

    private fun buildVariantGroupView(group: VariantGroup): View {
        val ctx = requireContext()
        val section = layoutInflater.inflate(R.layout.view_options_group, optionsContainer, false)
        val groupTitle = section.findViewById<TextView>(R.id.optionsGroupTitle)
        val groupHint = section.findViewById<TextView>(R.id.optionsGroupHint)
        val groupBody = section.findViewById<LinearLayout>(R.id.optionsGroupBody)

        groupTitle.text = group.title
        groupHint.isVisible = group.required
        groupHint.text = if (group.required) getString(R.string.required) else ""

        val radioGroup = RadioGroup(ctx).apply {
            orientation = RadioGroup.VERTICAL
        }

        val preselected = selectedVariantOptionIds[group.id]
        group.options.forEach { opt ->
            val rb = layoutInflater.inflate(R.layout.item_option_radio, radioGroup, false) as RadioButton
            rb.id = View.generateViewId()
            rb.text = buildOptionLabel(opt.title, opt.priceDeltaCents)
            rb.isChecked = preselected == opt.id
            rb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedVariantOptionIds[group.id] = opt.id
                    renderPrice()
                }
            }
            radioGroup.addView(rb)
        }

        // If required but nothing preselected, default to first.
        if (group.required && preselected == null) {
            val first = group.options.firstOrNull()
            if (first != null) {
                selectedVariantOptionIds[group.id] = first.id
                // Will be checked once views are drawn; we can also force-check first child.
                (radioGroup.getChildAt(0) as? RadioButton)?.isChecked = true
            }
        }

        groupBody.addView(radioGroup)
        return section
    }

    private fun buildAddOnGroupView(group: AddOnGroup): View {
        val section = layoutInflater.inflate(R.layout.view_options_group, optionsContainer, false)
        val groupTitle = section.findViewById<TextView>(R.id.optionsGroupTitle)
        val groupHint = section.findViewById<TextView>(R.id.optionsGroupHint)
        val groupBody = section.findViewById<LinearLayout>(R.id.optionsGroupBody)

        groupTitle.text = group.title

        val hint = buildAddOnHint(group)
        groupHint.isVisible = hint.isNotBlank()
        groupHint.text = hint

        group.options.forEach { opt ->
            val cb = layoutInflater.inflate(R.layout.item_option_checkbox, groupBody, false) as CheckBox
            cb.text = buildOptionLabel(opt.title, opt.priceDeltaCents)
            cb.isChecked = selectedAddOnOptionIds.contains(opt.id)

            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Max enforcement per group.
                    val selectedInGroupCount = group.options.count { selectedAddOnOptionIds.contains(it.id) }
                    val max = group.maxSelections
                    if (max != null && selectedInGroupCount >= max) {
                        // Revert selection and show error.
                        cb.isChecked = false
                        errorText.isVisible = true
                        errorText.text = getString(R.string.max_addons_reached, group.title, max)
                        return@setOnCheckedChangeListener
                    }
                    selectedAddOnOptionIds.add(opt.id)
                } else {
                    selectedAddOnOptionIds.remove(opt.id)
                }
                errorText.isVisible = false
                renderPrice()
            }
            groupBody.addView(cb)
        }

        return section
    }

    private fun buildOptionLabel(title: String, deltaCents: Int): String {
        return if (deltaCents == 0) title else {
            val sign = if (deltaCents > 0) "+" else "−"
            val abs = kotlin.math.abs(deltaCents)
            "$title ($sign${Formatters.moneyFromCents(abs)})"
        }
    }

    private fun buildAddOnHint(group: AddOnGroup): String {
        val min = group.minSelections
        val max = group.maxSelections
        if (!group.required && (min == 0) && max == null) return ""

        return when {
            group.required && max != null && min > 0 && min == max ->
                getString(R.string.choose_exactly, min)
            group.required && max != null && min > 0 ->
                getString(R.string.choose_between, min, max)
            group.required && min > 0 ->
                getString(R.string.choose_at_least, min)
            max != null ->
                getString(R.string.choose_up_to, max)
            else -> ""
        }
    }

    private fun renderPrice() {
        val configuration = ItemConfiguration(
            selectedVariantOptionIds = selectedVariantOptionIds.toMap(),
            selectedAddOnOptionIds = selectedAddOnOptionIds.toSet()
        )

        val unit = item.priceCents + computeOptionsDeltaCents(item, configuration)
        val total = unit * initialQuantity
        priceText.text = getString(
            R.string.line_price,
            Formatters.moneyFromCents(unit),
            initialQuantity,
            Formatters.moneyFromCents(total)
        )
    }

    private fun onConfirm() {
        errorText.isVisible = false

        // Validate required variants
        item.variantGroups.forEach { group ->
            if (group.required && selectedVariantOptionIds[group.id].isNullOrBlank()) {
                errorText.isVisible = true
                errorText.text = getString(R.string.please_choose_variant, group.title)
                return
            }
        }

        // Validate add-on min selections
        item.addOnGroups.forEach { group ->
            val selectedInGroup = group.options.count { selectedAddOnOptionIds.contains(it.id) }
            val min = group.minSelections.coerceAtLeast(if (group.required) 1 else 0)
            val max = group.maxSelections

            if (selectedInGroup < min) {
                errorText.isVisible = true
                errorText.text = getString(R.string.please_choose_addons_min, group.title, min)
                return
            }
            if (max != null && selectedInGroup > max) {
                errorText.isVisible = true
                errorText.text = getString(R.string.max_addons_reached, group.title, max)
                return
            }
        }

        val configuration = ItemConfiguration(
            selectedVariantOptionIds = selectedVariantOptionIds.toMap(),
            selectedAddOnOptionIds = selectedAddOnOptionIds.toSet()
        )

        val note = itemNoteEditText.text?.toString().orEmpty().trim().take(140)

        listener?.onConfirmed(item, configuration, initialQuantity, note)
        dismissAllowingStateLoss()
    }

    companion object {
        // PUBLIC_INTERFACE
        fun newInstance(): ItemOptionsBottomSheet {
            /** Create a new instance of the options bottom sheet. */
            return ItemOptionsBottomSheet()
        }
    }
}
