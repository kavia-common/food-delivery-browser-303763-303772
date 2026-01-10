package org.example.app.ui.ratings

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
import org.example.app.data.ratings.Review
import org.example.app.ui.common.MotionUtils

class ReviewsAdapter(
    private val onEdit: (Review) -> Unit,
    private val onDelete: (Review) -> Unit
) : ListAdapter<Review, ReviewsAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean = oldItem == newItem
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val author: TextView = itemView.findViewById(R.id.reviewAuthor)
        val rating: TextView = itemView.findViewById(R.id.reviewRating)
        val text: TextView = itemView.findViewById(R.id.reviewText)
        val edit: MaterialButton = itemView.findViewById(R.id.editReviewButton)
        val delete: MaterialButton = itemView.findViewById(R.id.deleteReviewButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val review = getItem(position)
        MotionUtils.animateListItemAppearIfNeeded(holder.itemView)

        holder.author.text = review.authorName
        holder.rating.text = "★ ${review.rating}"
        holder.rating.contentDescription = holder.itemView.context.getString(R.string.rating_content_description, review.rating)

        holder.text.isVisible = review.text.isNotBlank()
        holder.text.text = review.text

        holder.edit.setOnClickListener { onEdit(review) }
        holder.delete.setOnClickListener { onDelete(review) }
    }
}
