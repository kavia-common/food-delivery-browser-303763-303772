package org.example.app.common

import java.util.Locale

object Formatters {

    // PUBLIC_INTERFACE
    fun moneyFromCents(cents: Int): String {
        /** Format cents to $X.XX. */
        return String.format(Locale.US, "$%.2f", cents / 100.0)
    }

    // PUBLIC_INTERFACE
    fun ratingText(rating: Double): String {
        /** Format rating as "★ 4.2". */
        return "★ " + String.format(Locale.US, "%.1f", rating)
    }

    // PUBLIC_INTERFACE
    fun etaText(min: Int, max: Int): String {
        /** Format ETA as "25–35 min". */
        return "$min–$max min"
    }
}
