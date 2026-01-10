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
import org.example.app.data.ratings.RatingsRepository
import org.example.app.data.ratings.ReviewTarget
import org.example.app.data.ratings.ReviewTargetType
import org.example.app.ui.common.MotionUtils

class MenuItemAdapter(
    private val onItemClick: (MenuItem) -> Unit,
    private val onAdd: (MenuItem) -> Unit,
    private val onInc: (MenuItem) -> Unit,
    private val onDec: (MenuItem) -> Unit,
    private val getQuantity: (String) -> Int,
    private val isFavorited: (String) -> Boolean,
    private val onToggleFavorite: (String) -> Unit,
    private val onOpenReviews: (MenuItem) -> Unit
) : ListAdapter<MenuItem, MenuItemAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<MenuItem>() {
        override fun areItemsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MenuItem, newItem: MenuItem): Boolean = oldItem == newItem
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vegIcon: ImageView = itemView.findViewById(R.id.vegIcon)
        val name: TextView = itemView.findViewById(R.id.menuItemName)
        val desc: TextView = itemView.findViewById(R.id.menuItemDesc)
        val ratingMeta: TextView = itemView.findViewById(R.id.menuItemRatingMeta)
        val price: TextView = itemView.findViewById(R.id.menuItemPrice)
        val customizableHint: TextView = itemView.findViewById(R.id.menuItemCustomizableHint)

        val menuItemImage: ImageView = itemView.findViewById(R.id.menuItemImage)
        val favoriteToggle: ImageView = itemView.findViewById(R.id.favoriteToggle)

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

        MotionUtils.animateListItemAppearIfNeeded(holder.itemView)

        holder.name.text = item.name
        holder.desc.text = item.description
        holder.price.text = Formatters.moneyFromCents(item.priceCents)
        holder.vegIcon.setImageResource(if (item.isVeg) R.drawable.ic_veg else R.drawable.ic_nonveg)

        // No external image loading in this demo; show local placeholder.
        // imageUrl is kept for forward compatibility / future local mapping.
        holder.menuItemImage.setImageResource(R.drawable.ic_placeholder_food)
        holder.menuItemImage.contentDescription =
            holder.itemView.context.getString(R.string.menu_item_image)

        val hasOptions = item.variantGroups.isNotEmpty() || item.addOnGroups.isNotEmpty()
        holder.customizableHint.isVisible = hasOptions
        holder.customizableHint.text = holder.itemView.context.getString(R.string.customizable)

        // Optional rating snippet for menu item if user reviews exist.
        val agg = RatingsRepository.getAggregateNow(ReviewTarget(ReviewTargetType.MENU_ITEM, item.id))
        if (agg != null && agg.count > 0) {
            holder.ratingMeta.isVisible = true
            holder.ratingMeta.text = "${Formatters.ratingText(agg.average)} ★ (${agg.count})"
            holder.ratingMeta.contentDescription =
                holder.itemView.context.getString(R.string.based_on_reviews, agg.count)
            holder.ratingMeta.setOnClickListener { onOpenReviews(item) }
        } else {
            holder.ratingMeta.isVisible = false
            holder.ratingMeta.setOnClickListener(null)
        }

        val fav = isFavorited(item.id)
        holder.favoriteToggle.setImageResource(if (fav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
        holder.favoriteToggle.contentDescription = holder.itemView.context.getString(R.string.favorite)
        holder.favoriteToggle.setOnClickListener {
            onToggleFavorite(item.id)
            val nowFav = isFavorited(item.id)
            holder.favoriteToggle.setImageResource(if (nowFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
            holder.favoriteToggle.contentDescription = holder.itemView.context.getString(R.string.favorite)
        }

        val q = getQuantity(item.id) // total across configurations
        holder.addButton.isVisible = q <= 0
        holder.qtyContainer.isVisible = q > 0
        holder.qtyText.text = q.toString()

        holder.addButton.setOnClickListener {
            MotionUtils.animateTapBounce(holder.addButton)
            MotionUtils.performHapticClick(holder.addButton)
            onAdd(item)
        }
        holder.plusButton.setOnClickListener {
            MotionUtils.animateTapBounce(holder.plusButton)
            MotionUtils.performHapticClick(holder.plusButton)
            onInc(item)
        }
        holder.minusButton.setOnClickListener {
            MotionUtils.animateTapBounce(holder.minusButton)
            MotionUtils.performHapticClick(holder.minusButton)
            onDec(item)
        }

        // Tap anywhere on row to customize/add.
        holder.itemView.setOnClickListener { onItemClick(item) }
    }
}
