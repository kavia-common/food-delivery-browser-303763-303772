package org.example.app.ui.delivery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.data.delivery.DeliveryStage

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
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.stageIcon)
        private val label: TextView = itemView.findViewById(R.id.stageLabel)
        private val time: TextView = itemView.findViewById(R.id.stageTime)
        private val eta: TextView = itemView.findViewById(R.id.stageEta)
        private val line: View = itemView.findViewById(R.id.verticalLine)

        fun bind(row: DeliveryTimelineRow, isLast: Boolean) {
            label.text = row.stage.displayLabel()
            time.text = row.timeText
            eta.isVisible = !row.etaText.isNullOrBlank()
            eta.text = row.etaText ?: ""

            // Use theme-aware icons and tint; keep simple with existing drawables.
            icon.setImageResource(
                when {
                    row.isCompleted -> R.drawable.ic_check_circle
                    row.isCurrent -> R.drawable.ic_timer
                    else -> R.drawable.ic_timer
                }
            )
            icon.alpha = when {
                row.isCompleted -> 1.0f
                row.isCurrent -> 1.0f
                else -> 0.5f
            }

            line.isVisible = !isLast
            line.alpha = if (row.isCompleted) 1.0f else 0.4f
        }
    }
}
