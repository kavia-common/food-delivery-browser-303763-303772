package org.example.app.ui.ratings

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputLayout
import org.example.app.R
import org.example.app.data.ratings.RatingsRepository
import org.example.app.data.ratings.Review
import org.example.app.data.ratings.ReviewDraft
import org.example.app.data.ratings.ReviewTarget
import org.example.app.data.ratings.ReviewTargetType
import org.example.app.data.ratings.ReviewUpdate
import kotlin.math.roundToInt

class ReviewEditorDialogFragment : DialogFragment() {

    private var mode: Mode = Mode.ADD
    private lateinit var target: ReviewTarget
    private var existingReviewId: String? = null

    enum class Mode { ADD, EDIT }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        mode = Mode.valueOf(args.getString(ARG_MODE) ?: Mode.ADD.name)
        val type = ReviewTargetType.valueOf(args.getString(ARG_TARGET_TYPE)!!)
        val id = args.getString(ARG_TARGET_ID)!!
        target = ReviewTarget(type, id)
        existingReviewId = args.getString(ARG_REVIEW_ID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = requireContext()
        val v = LayoutInflater.from(ctx).inflate(R.layout.dialog_review_editor, null, false)

        val nameLayout = v.findViewById<TextInputLayout>(R.id.nameLayout)
        val nameEdit = v.findViewById<EditText>(R.id.nameEdit)

        val ratingValue = v.findViewById<TextView>(R.id.ratingValue)
        val ratingSlider = v.findViewById<Slider>(R.id.ratingSlider)

        val textLayout = v.findViewById<TextInputLayout>(R.id.reviewTextLayout)
        val textEdit = v.findViewById<EditText>(R.id.reviewTextEdit)

        // Prefill when editing.
        val reviewToEdit: Review? = existingReviewId?.let { idToFind ->
            RatingsRepository.getReviews(target).value?.firstOrNull { it.id == idToFind }
                ?: RatingsRepository.getReviews(target).value?.firstOrNull()
        }

        if (mode == Mode.EDIT && reviewToEdit != null) {
            nameEdit.setText(reviewToEdit.authorName)
            ratingSlider.value = reviewToEdit.rating.toFloat()
            textEdit.setText(reviewToEdit.text)
        }

        fun updateRatingLabel() {
            val r = ratingSlider.value.roundToInt().coerceIn(0, 5)
            ratingValue.text = ctx.getString(R.string.rating_value, r)
            ratingValue.contentDescription = ctx.getString(R.string.rating_content_description, r)
        }
        updateRatingLabel()

        ratingSlider.addOnChangeListener { _, _, _ -> updateRatingLabel() }

        // Basic max length hint/validation (TextInputLayout counter handles visual if enabled).
        textEdit.addTextChangedListener {
            if ((it?.length ?: 0) > MAX_REVIEW_LEN) {
                textLayout.error = ctx.getString(R.string.review_too_long, MAX_REVIEW_LEN)
            } else {
                textLayout.error = null
            }
        }

        val title = if (mode == Mode.EDIT) ctx.getString(R.string.edit_review) else ctx.getString(R.string.add_review)

        return androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle(title)
            .setView(v)
            .setPositiveButton(R.string.submit, null) // override later for validation
            .setNegativeButton(R.string.cancel, null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    val btn = (dialog as androidx.appcompat.app.AlertDialog).getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                    btn.setOnClickListener {
                        val author = nameEdit.text?.toString().orEmpty().trim()
                        val rating = ratingSlider.value.roundToInt()
                        val text = textEdit.text?.toString().orEmpty()

                        nameLayout.error = null
                        ratingValue.error = null
                        textLayout.error = null

                        var ok = true
                        if (author.isBlank()) {
                            nameLayout.error = ctx.getString(R.string.your_name_required)
                            ok = false
                        }
                        if (rating < 1) {
                            ratingValue.error = ctx.getString(R.string.rating_required)
                            ok = false
                        }
                        if (text.length > MAX_REVIEW_LEN) {
                            textLayout.error = ctx.getString(R.string.review_too_long, MAX_REVIEW_LEN)
                            ok = false
                        }
                        if (!ok) return@setOnClickListener

                        if (mode == Mode.EDIT && existingReviewId != null) {
                            RatingsRepository.updateReview(
                                reviewId = existingReviewId!!,
                                changes = ReviewUpdate(authorName = author, rating = rating, text = text)
                            )
                        } else {
                            RatingsRepository.addReview(target, ReviewDraft(authorName = author, rating = rating, text = text))
                        }
                        dismiss()
                    }
                }
            }
    }

    companion object {
        private const val ARG_MODE = "mode"
        private const val ARG_TARGET_TYPE = "target_type"
        private const val ARG_TARGET_ID = "target_id"
        private const val ARG_REVIEW_ID = "review_id"

        private const val MAX_REVIEW_LEN = 280

        // PUBLIC_INTERFACE
        fun newAdd(target: ReviewTarget): ReviewEditorDialogFragment {
            /** Create dialog for adding a new review for a target. */
            return ReviewEditorDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, Mode.ADD.name)
                    putString(ARG_TARGET_TYPE, target.type.name)
                    putString(ARG_TARGET_ID, target.id)
                }
            }
        }

        // PUBLIC_INTERFACE
        fun newEdit(target: ReviewTarget, reviewId: String): ReviewEditorDialogFragment {
            /** Create dialog for editing an existing review by id. */
            return ReviewEditorDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, Mode.EDIT.name)
                    putString(ARG_TARGET_TYPE, target.type.name)
                    putString(ARG_TARGET_ID, target.id)
                    putString(ARG_REVIEW_ID, reviewId)
                }
            }
        }
    }
}
