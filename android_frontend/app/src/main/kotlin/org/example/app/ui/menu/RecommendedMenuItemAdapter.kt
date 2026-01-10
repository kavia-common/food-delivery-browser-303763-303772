package org.example.app.ui.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.common.Formatters
import org.example.app.data.models.MenuItem
import org.example.app.ui.common.MotionUtils

class RecommendedMenuItemAdapter(
    private val onClick: (MenuItem) -> Unit
) : ListAdapter<MenuItem, RecommendedMenuItemAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<MenuItem>() {
        override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean = oldItem == newItem
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.itemImage)
        val name: TextView = itemView.findViewById(R.id.itemName)
        val meta: TextView = itemView.findViewById(R.id.itemMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_menu_compact, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        MotionUtils.animateListItemAppearIfNeeded(holder.itemView)

        holder.image.setImageResource(R.drawable.ic_placeholder_food)
        holder.name.text = item.name
        holder.meta.text = Formatters.moneyFromCents(item.priceCents)

        holder.itemView.contentDescription =
            holder.itemView.context.getString(
                R.string.cd_recommended_menu_item_full,
                item.name,
                Formatters.moneyFromCents(item.priceCents)
            )

        holder.itemView.setOnClickListener { onClick(item) }
    }
}
