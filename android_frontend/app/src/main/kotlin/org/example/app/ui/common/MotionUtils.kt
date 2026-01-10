package org.example.app.ui.common

import android.animation.TimeInterpolator
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityManagerCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * PUBLIC_INTERFACE
 * Small motion + haptics utilities used across the app.
 *
 * All motion helpers are built to be subtle and to respect reduced-motion settings where possible.
 */
object MotionUtils {

    /**
     * PUBLIC_INTERFACE
     * Returns true when the system is likely configured to reduce/disable animations.
     *
     * Notes:
     * - Android does not have a single official "reduced motion" flag across all versions.
     * - We conservatively check animation scales (global) and accessibility touch exploration.
     */
    fun isReducedMotionEnabled(context: Context): Boolean {
        val scale = getAnimatorDurationScale(context)
        if (scale <= 0f) return true

        // If touch exploration is enabled, prefer calmer UI (less motion).
        val am = context.getSystemService<AccessibilityManager>()
        val touchExploration = am?.let { AccessibilityManagerCompat.isTouchExplorationEnabled(it) } ?: false
        return touchExploration
    }

    /**
     * PUBLIC_INTERFACE
     * Subtle "appear" animation for RecyclerView rows.
     *
     * Performance notes:
     * - Only animates first bind per view holder (tracked via a tag).
     * - Uses ViewPropertyAnimator (hardware accelerated) and short duration.
     */
    fun animateListItemAppearIfNeeded(itemView: View) {
        val context = itemView.context
        if (isReducedMotionEnabled(context)) return

        val tagKey = org.example.app.R.id.tag_has_appeared
        val already = itemView.getTag(tagKey) as? Boolean ?: false
        if (already) return

        itemView.setTag(tagKey, true)

        // Small fade + scale-in, tuned for 60fps even on long lists.
        itemView.alpha = 0f
        itemView.scaleX = 0.98f
        itemView.scaleY = 0.98f
        itemView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(140L)
            .setInterpolator(defaultInterpolator())
            .start()
    }

    /**
     * PUBLIC_INTERFACE
     * Subtle bounce for small tap actions (e.g., add-to-cart, +/- quantity).
     */
    fun animateTapBounce(view: View) {
        val context = view.context
        if (isReducedMotionEnabled(context)) return

        view.animate().cancel()
        view.scaleX = 1f
        view.scaleY = 1f
        view.animate()
            .scaleX(0.94f)
            .scaleY(0.94f)
            .setDuration(70L)
            .setInterpolator(defaultInterpolator())
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(110L)
                    .setInterpolator(defaultInterpolator())
                    .start()
            }
            .start()
    }

    /**
     * PUBLIC_INTERFACE
     * Performs lightweight haptic feedback for click-like actions where available.
     */
    fun performHapticClick(view: View) {
        // Avoid exceptions on old devices; View#performHapticFeedback is safe.
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.KEYBOARD_TAP
        )
    }

    /**
     * PUBLIC_INTERFACE
     * Applies subtle diff/change animations (move/change) to RecyclerViews via DefaultItemAnimator.
     *
     * We keep durations short to stay within frame budget.
     */
    fun createSubtleItemAnimator(context: Context): RecyclerView.ItemAnimator? {
        if (isReducedMotionEnabled(context)) {
            // Respect reduced motion: disable change/move animations.
            return null
        }

        return DefaultItemAnimator().apply {
            supportsChangeAnimations = false
            addDuration = 120L
            removeDuration = 110L
            moveDuration = 160L
            changeDuration = 120L
        }
    }

    private fun defaultInterpolator(): TimeInterpolator = FastOutSlowInInterpolator()

    private fun getAnimatorDurationScale(context: Context): Float {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            } else {
                @Suppress("DEPRECATION")
                Settings.System.getFloat(context.contentResolver, Settings.System.ANIMATOR_DURATION_SCALE, 1f)
            }
        } catch (_: Throwable) {
            1f
        }
    }
}
