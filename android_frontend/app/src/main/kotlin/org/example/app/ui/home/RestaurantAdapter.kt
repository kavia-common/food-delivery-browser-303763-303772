package org.example.app.ui.home

import android.graphics.Color
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
import org.example.app.data.models.Restaurant
import org.example.app.ui.common.MotionUtils
import org.example.app.data.ratings.RatingsRepository
import org.example.app.data.ratings.ReviewTarget
import org.example.app.data.ratings.ReviewTargetType

class RestaurantAdapter(
    private val onClick: (Restaurant) -> Unit,
    private val isFavorited: (String) -> Boolean,
    private val onToggleFavorite: (String) -> Unit
) : ListAdapter<Restaurant, RestaurantAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Restaurant>() {
        override fun areItemsTheSame(oldItem: Restaurant, newItem: Restaurant): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Restaurant, newItem: Restaurant): Boolean =
            oldItem == newItem
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val banner: View = itemView.findViewById(R.id.banner)
        val restaurantImage: ImageView = itemView.findViewById(R.id.restaurantImage)
        val name: TextView = itemView.findViewById(R.id.restaurantName)
        val tags: TextView = itemView.findViewById(R.id.restaurantTags)
        val rating: TextView = itemView.findViewById(R.id.restaurantRating)
        val eta: TextView = itemView.findViewById(R.id.restaurantEta)
        val favoriteToggle: ImageView = itemView.findViewById(R.id.favoriteToggle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_restaurant, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val restaurant = getItem(position)

        MotionUtils.animateListItemAppearIfNeeded(holder.itemView)

        holder.name.text = restaurant.name
        holder.tags.text = restaurant.cuisineTags.joinToString(" • ")

        // No external image loading in this demo; always show a local placeholder.
        // imageUrl is kept for forward compatibility / future local mapping.
        holder.restaurantImage.setImageResource(R.drawable.ic_placeholder_restaurant)
        holder.restaurantImage.contentDescription =
            holder.itemView.context.getString(R.string.restaurant_image)

        val agg = RatingsRepository.getAggregateNow(ReviewTarget(ReviewTargetType.RESTAURANT, restaurant.id))
        holder.rating.text = if (agg != null && agg.count > 0) {
            // Example: "4.6 ★ (12)"
            "${Formatters.ratingText(agg.average)} ★ (${agg.count})"
        } else {
            // Fall back to mock rating when user hasn't reviewed yet.
            Formatters.ratingText(restaurant.rating)
        }

        holder.eta.text = Formatters.etaText(restaurant.etaMinutesMin, restaurant.etaMinutesMax)

        try {
            holder.banner.setBackgroundColor(Color.parseColor(restaurant.bannerColorHex))
        } catch (_: Throwable) {
            holder.banner.setBackgroundColor(Color.LTGRAY)
        }

        val fav = isFavorited(restaurant.id)
        holder.favoriteToggle.setImageResource(if (fav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
        holder.favoriteToggle.contentDescription = holder.itemView.context.getString(R.string.favorite)

        holder.favoriteToggle.setOnClickListener {
            onToggleFavorite(restaurant.id)
            // Optimistic UI update; fragments also observe repository and may re-submit list.
            val nowFav = isFavorited(restaurant.id)
            holder.favoriteToggle.setImageResource(if (nowFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
            holder.favoriteToggle.contentDescription = holder.itemView.context.getString(R.string.favorite)
        }

        holder.itemView.setOnClickListener { onClick(restaurant) }
    }
}
