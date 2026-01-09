package org.example.app.ui.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.example.app.R
import org.example.app.common.Formatters
import org.example.app.data.models.MenuItem

class MenuItemAdapter(
    private val onAdd: (MenuItem) -> Unit,
    private val onInc: (MenuItem) -> Unit,
    private val onDec: (MenuItem) -> Unit,
    private val getQuantity: (String) -> Int
) : ListAdapter<MenuItem, MenuItemAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<MenuItem>() {
        override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean = oldItem == newItem
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vegIcon: ImageView = itemView.findViewById(R.id.vegIcon)
        val name: TextView = itemView.findViewById(R.id.menuItemName)
        val desc: TextView = itemView.findViewById(R.id.menuItemDesc)
        val price: TextView = itemView.findViewById(R.id.menuItemPrice)

        val addButton: MaterialButton = itemView.findViewById(R.id.addButton)

        val qtyContainer: View = itemView.findViewById(R.id.qtyContainer)
        val minusButton: MaterialButton = itemView.findViewById(R.id.minusButton)
        val plusButton: MaterialButton = itemView.findViewById(R.id.plusButton)
        val qtyText: TextView = itemView.findViewById(R.id.qtyText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_menu, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        holder.name.text = item.name
        holder.desc.text = item.description
        holder.price.text = Formatters.moneyFromCents(item.priceCents)
        holder.vegIcon.setImageResource(if (item.isVeg) R.drawable.ic_veg else R.drawable.ic_nonveg)

        val q = getQuantity(item.id)
        holder.addButton.isVisible = q <= 0
        holder.qtyContainer.isVisible = q > 0
        holder.qtyText.text = q.toString()

        holder.addButton.setOnClickListener { onAdd(item) }
        holder.plusButton.setOnClickListener { onInc(item) }
        holder.minusButton.setOnClickListener { onDec(item) }
    }
}
