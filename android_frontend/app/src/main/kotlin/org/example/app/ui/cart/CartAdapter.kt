package org.example.app.ui.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.example.app.R
import org.example.app.common.Formatters
import org.example.app.data.models.CartLine

class CartAdapter(
    private val onInc: (CartLine) -> Unit,
    private val onDec: (CartLine) -> Unit,
    private val onRemove: (CartLine) -> Unit
) : ListAdapter<CartLine, CartAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<CartLine>() {
        override fun areItemsTheSame(oldItem: CartLine, newItem: CartLine): Boolean =
            oldItem.item.id == newItem.item.id

        override fun areContentsTheSame(oldItem: CartLine, newItem: CartLine): Boolean =
            oldItem == newItem
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.cartItemName)
        val priceEach: TextView = itemView.findViewById(R.id.cartItemPriceEach)
        val lineTotal: TextView = itemView.findViewById(R.id.cartItemLineTotal)

        val minus: MaterialButton = itemView.findViewById(R.id.cartMinusButton)
        val plus: MaterialButton = itemView.findViewById(R.id.cartPlusButton)
        val qty: TextView = itemView.findViewById(R.id.cartQtyText)
        val remove: MaterialButton = itemView.findViewById(R.id.cartRemoveButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val line = getItem(position)

        holder.name.text = line.item.name
        holder.priceEach.text = Formatters.moneyFromCents(line.item.priceCents)
        holder.qty.text = line.quantity.toString()
        holder.lineTotal.text = Formatters.moneyFromCents(line.item.priceCents * line.quantity)

        holder.plus.setOnClickListener { onInc(line) }
        holder.minus.setOnClickListener { onDec(line) }
        holder.remove.setOnClickListener { onRemove(line) }
    }
}
