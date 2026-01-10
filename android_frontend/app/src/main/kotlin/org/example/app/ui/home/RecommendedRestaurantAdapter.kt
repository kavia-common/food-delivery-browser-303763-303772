package org.example.app.ui.home

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

class RecommendedRestaurantAdapter(
    private val onClick: (Restaurant) -> Unit
) : ListAdapter<Restaurant, RecommendedRestaurantAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Restaurant>() {
        override fun areItemsTheSame(oldItem: Restaurant, newItem: Restaurant): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Restaurant, newItem: Restaurant): Boolean = oldItem == newItem
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.restaurantImage)
        val name: TextView = itemView.findViewById(R.id.restaurantName)
        val meta: TextView = itemView.findViewById(R.id.restaurantMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_restaurant_compact, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = getItem(position)
        MotionUtils.animateListItemAppearIfNeeded(holder.itemView)

        holder.image.setImageResource(R.drawable.ic_placeholder_restaurant)
        holder.name.text = r.name
        holder.meta.text = "${Formatters.ratingText(r.rating)} • ${Formatters.etaText(r.etaMinutesMin, r.etaMinutesMax)}"

        holder.itemView.contentDescription =
            holder.itemView.context.getString(R.string.cd_recommended_restaurant, r.name)

        holder.itemView.setOnClickListener { onClick(r) }
    }
}
