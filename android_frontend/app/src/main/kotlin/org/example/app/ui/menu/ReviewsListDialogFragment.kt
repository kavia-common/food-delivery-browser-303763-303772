package org.example.app.ui.menu

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.example.app.R
import org.example.app.common.Formatters
import org.example.app.data.ratings.RatingsRepository
import org.example.app.data.ratings.ReviewTarget
import org.example.app.data.ratings.ReviewTargetType
import org.example.app.ui.common.MotionUtils
import org.example.app.ui.ratings.ReviewEditorDialogFragment
import org.example.app.ui.ratings.ReviewsAdapter

class ReviewsListDialogFragment : DialogFragment() {

    private lateinit var target: ReviewTarget
    private var titleText: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        titleText = args.getString(ARG_TITLE).orEmpty()
        val type = ReviewTargetType.valueOf(args.getString(ARG_TARGET_TYPE)!!)
        val id = args.getString(ARG_TARGET_ID)!!
        target = ReviewTarget(type, id)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireContext()
        val v = LayoutInflater.from(ctx).inflate(R.layout.dialog_reviews_list, null, false)

        val summary = v.findViewById<TextView>(R.id.dialogReviewsSummary)
        val noReviews = v.findViewById<TextView>(R.id.dialogNoReviewsHint)
        val addBtn = v.findViewById<MaterialButton>(R.id.dialogAddReviewButton)

        val list = v.findViewById<RecyclerView>(R.id.dialogReviewsRecyclerView)
        val adapter = ReviewsAdapter(
            onEdit = { r ->
                ReviewEditorDialogFragment.newEdit(target, r.id)
                    .show(parentFragmentManager, "ReviewEditorDialogFragment")
            },
            onDelete = { r ->
                androidx.appcompat.app.AlertDialog.Builder(ctx)
                    .setTitle(getString(R.string.delete_review_title))
                    .setMessage(getString(R.string.delete_review_message))
                    .setPositiveButton(R.string.delete) { _, _ -> RatingsRepository.deleteReview(r.id) }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        )
        list.layoutManager = LinearLayoutManager(ctx)
        list.adapter = adapter
        list.itemAnimator = MotionUtils.createSubtleItemAnimator(ctx)

        addBtn.setOnClickListener {
            ReviewEditorDialogFragment.newAdd(target)
                .show(parentFragmentManager, "ReviewEditorDialogFragment")
        }

        RatingsRepository.getAggregate(target).observe(this) { agg ->
            summary.text = if (agg != null && agg.count > 0) {
                "${Formatters.ratingText(agg.average)} ★ • " + getString(R.string.based_on_reviews, agg.count)
            } else {
                getString(R.string.no_reviews_summary)
            }
        }
        RatingsRepository.getReviews(target).observe(this) { reviews ->
            val r = reviews ?: emptyList()
            adapter.submitList(r)
            noReviews.isVisible = r.isEmpty()
        }

        return androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle(titleText.ifBlank { getString(R.string.reviews) })
            .setView(v)
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_TARGET_TYPE = "target_type"
        private const val ARG_TARGET_ID = "target_id"

        // PUBLIC_INTERFACE
        fun newInstance(title: String, targetType: ReviewTargetType, targetId: String): ReviewsListDialogFragment {
            /** Create a reviews list dialog for a given target. */
            return ReviewsListDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_TARGET_TYPE, targetType.name)
                    putString(ARG_TARGET_ID, targetId)
                }
            }
        }
    }
}
