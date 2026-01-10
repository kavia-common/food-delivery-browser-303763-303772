package org.example.app.ui.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.example.app.R
import org.example.app.common.Formatters
import org.example.app.data.models.CartLine
import org.example.app.data.models.buildConfigurationSummary
import org.example.app.data.models.computeOptionsDeltaCents
import org.example.app.ui.common.MotionUtils

class CartAdapter(
    private val onInc: (CartLine) -> Unit,
    private val onDec: (CartLine) -> Unit,
    private val onRemove: (CartLine) -> Unit,
    private val onEdit: (CartLine) -> Unit
) : ListAdapter<CartLine, CartAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<CartLine>() {
        override fun areItemsTheSame(oldItem: CartLine, newItem: CartLine): Boolean =
            oldItem.item.id == newItem.item.id && oldItem.configuration.stableKey() == newItem.configuration.stableKey()

        override fun areContentsTheSame(oldItem: CartLine, newItem: CartLine): Boolean =
            oldItem == newItem
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.cartItemName)
        val options: TextView = itemView.findViewById(R.id.cartItemOptions)
        val note: TextView = itemView.findViewById(R.id.cartItemNote)
        val priceEach: TextView = itemView.findViewById(R.id.cartItemPriceEach)
        val lineTotal: TextView = itemView.findViewById(R.id.cartItemLineTotal)

        val minus: MaterialButton = itemView.findViewById(R.id.cartMinusButton)
        val plus: MaterialButton = itemView.findViewById(R.id.cartPlusButton)
        val qty: TextView = itemView.findViewById(R.id.cartQtyText)
        val edit: MaterialButton = itemView.findViewById(R.id.cartEditButton)
        val remove: MaterialButton = itemView.findViewById(R.id.cartRemoveButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val line = getItem(position)

        MotionUtils.animateListItemAppearIfNeeded(holder.itemView)

        holder.name.text = line.item.name

        val summary = buildConfigurationSummary(line.item, line.configuration)
        holder.options.isVisible = summary.isNotBlank()
        holder.options.text = summary

        val note = line.itemNote.trim()
        holder.note.isVisible = note.isNotBlank()
        holder.note.text = if (note.isNotBlank()) {
            holder.itemView.context.getString(R.string.item_note_prefix, note)
        } else {
            ""
        }

        val unitPrice = line.item.priceCents + computeOptionsDeltaCents(line.item, line.configuration)
        holder.priceEach.text = Formatters.moneyFromCents(unitPrice)
        holder.qty.text = line.quantity.toString()
        holder.lineTotal.text = Formatters.moneyFromCents(unitPrice * line.quantity)

        // TalkBack labels for row actions.
        holder.plus.contentDescription =
            holder.itemView.context.getString(R.string.cd_increase_quantity_for_item, line.item.name)
        holder.minus.contentDescription =
            holder.itemView.context.getString(R.string.cd_decrease_quantity_for_item, line.item.name)
        holder.edit.contentDescription =
            holder.itemView.context.getString(R.string.cd_edit_item, line.item.name)
        holder.remove.contentDescription =
            holder.itemView.context.getString(R.string.cd_remove_item, line.item.name)

        holder.plus.setOnClickListener {
            MotionUtils.animateTapBounce(holder.plus)
            MotionUtils.performHapticClick(holder.plus)
            onInc(line)
        }
        holder.minus.setOnClickListener {
            MotionUtils.animateTapBounce(holder.minus)
            MotionUtils.performHapticClick(holder.minus)
            onDec(line)
        }
        holder.edit.setOnClickListener { onEdit(line) }
        holder.remove.setOnClickListener { onRemove(line) }

        // Tap row to edit options/notes (if available).
        holder.itemView.setOnClickListener { onEdit(line) }
    }
}
