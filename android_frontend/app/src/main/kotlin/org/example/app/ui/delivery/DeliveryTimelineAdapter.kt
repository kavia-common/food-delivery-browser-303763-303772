package org.example.app.ui.delivery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.data.delivery.DeliveryStage
import org.example.app.ui.common.MotionUtils

data class DeliveryTimelineRow(
    val stage: DeliveryStage,
    val isCompleted: Boolean,
    val isCurrent: Boolean,
    val timeText: String,
    val etaText: String?
)

class DeliveryTimelineAdapter : RecyclerView.Adapter<DeliveryTimelineAdapter.VH>() {

    private val items: MutableList<DeliveryTimelineRow> = mutableListOf()

    fun submit(rows: List<DeliveryTimelineRow>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_delivery_timeline, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], position == items.size - 1)
        MotionUtils.animateListItemAppearIfNeeded(holder.itemView)
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.stageIcon)
        private val label: TextView = itemView.findViewById(R.id.stageLabel)
        private val time: TextView = itemView.findViewById(R.id.stageTime)
        private val eta: TextView = itemView.findViewById(R.id.stageEta)
        private val line: View = itemView.findViewById(R.id.verticalLine)

        fun bind(row: DeliveryTimelineRow, isLast: Boolean) {
            val context = itemView.context

            label.text = row.stage.displayLabel()
            time.text = row.timeText
            eta.isVisible = !row.etaText.isNullOrBlank()
            eta.text = row.etaText ?: ""

            val inactiveColor = ContextCompat.getColor(context, R.color.delivery_timeline_inactive)
            val activeColor = ContextCompat.getColor(context, R.color.delivery_timeline_active)
            val completedColor = ContextCompat.getColor(context, R.color.delivery_timeline_completed)

            val (iconRes, tintColor) = when {
                row.isCompleted -> R.drawable.ic_check_circle to completedColor
                row.isCurrent -> R.drawable.ic_timer to activeColor
                else -> R.drawable.ic_timer to inactiveColor
            }

            icon.setImageResource(iconRes)
            icon.imageTintList = ColorStateList.valueOf(tintColor)
            icon.alpha = if (row.isCompleted || row.isCurrent) 1.0f else 0.65f

            line.isVisible = !isLast
            if (!isLast) {
                val connectorDrawable = when {
                    row.isCompleted -> R.drawable.delivery_timeline_connector_completed
                    row.isCurrent -> R.drawable.delivery_timeline_connector_active
                    else -> R.drawable.delivery_timeline_connector_inactive
                }
                line.setBackgroundResource(connectorDrawable)
                line.alpha = if (row.isCompleted || row.isCurrent) 1.0f else 0.8f
            }

            // Accessibility: stage status with timestamp (or pending) + step position.
            val statusText = when {
                row.isCompleted -> context.getString(R.string.delivery_status_completed)
                row.isCurrent -> context.getString(R.string.delivery_status_in_progress)
                else -> context.getString(R.string.delivery_status_pending)
            }
            val etaSuffix = row.etaText?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""
            val step = adapterPosition + 1
            val totalSteps = (bindingAdapter as? DeliveryTimelineAdapter)?.itemCount ?: 0
            itemView.contentDescription = context.getString(
                R.string.cd_delivery_timeline_row,
                row.stage.displayLabel(),
                step,
                totalSteps,
                statusText,
                row.timeText,
                etaSuffix
            )

            // Micro-animations when a stage becomes completed/current (once per row/stage).
            animateStageAdvanceIfNeeded(row)
        }

        private fun animateStageAdvanceIfNeeded(row: DeliveryTimelineRow) {
            val context = itemView.context
            if (MotionUtils.isReducedMotionEnabled(context)) return

            // We only animate when a row is "not pending" (i.e., completed/current),
            // and only once per ViewHolder instance.
            val tagKey = R.id.tag_delivery_stage_advanced
            val already = itemView.getTag(tagKey) as? Boolean ?: false
            if (already) return
            if (!row.isCompleted && !row.isCurrent) return

            itemView.setTag(tagKey, true)

            // Icon pop + connector fade in (subtle).
            icon.scaleX = 0.9f
            icon.scaleY = 0.9f
            icon.alpha = 0.0f
            val iconScaleX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 0.9f, 1.0f)
            val iconScaleY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 0.9f, 1.0f)
            val iconAlpha = ObjectAnimator.ofFloat(icon, View.ALPHA, 0.0f, 1.0f)

            val lineAlpha = ObjectAnimator.ofFloat(line, View.ALPHA, 0.0f, line.alpha)

            AnimatorSet().apply {
                playTogether(iconScaleX, iconScaleY, iconAlpha, lineAlpha)
                duration = 160L
                start()
            }
        }
    }
}
