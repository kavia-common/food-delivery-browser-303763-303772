package org.example.app.data.ratings

/**
 * Safe, stable codec for ratings & reviews persistence.
 *
 * Encoding:
 * - Reviews list: newline-delimited lines
 *   "id|targetType|targetId|authorName|rating|text|createdAtMs|updatedAtMs"
 * - Aggregates map: newline-delimited lines
 *   "targetType|targetId|average|count"
 *
 * Notes:
 * - Uses escaping compatible with SafeCodec style (pipe + newline escaping).
 * - Decoder is defensive and ignores malformed rows.
 */
internal object RatingsCodec {
    private const val FIELD_SEP = '|'
    private const val LINE_SEP = '\n'
    private const val ESC = '\\'

    // PUBLIC_INTERFACE
    fun encodeReviews(reviews: List<Review>): String {
        /** Encode a list of reviews to a single string for SharedPreferences. */
        return reviews.joinToString(separator = LINE_SEP.toString()) { r ->
            listOf(
                r.id,
                r.targetType.name,
                r.targetId,
                r.authorName,
                r.rating.toString(),
                r.text,
                r.createdAtMs.toString(),
                r.updatedAtMs.toString()
            ).joinToString(separator = FIELD_SEP.toString()) { escape(it) }
        }
    }

    // PUBLIC_INTERFACE
    fun decodeReviews(encoded: String?): List<Review> {
        /** Decode persisted reviews string; returns empty list on missing/malformed data. */
        if (encoded.isNullOrBlank()) return emptyList()

        return try {
            encoded.split(LINE_SEP)
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { line ->
                    try {
                        val parts = splitEscaped(line, FIELD_SEP)
                        if (parts.size < 8) return@mapNotNull null

                        val id = unescape(parts[0]).trim()
                        val typeStr = unescape(parts[1]).trim()
                        val targetId = unescape(parts[2]).trim()
                        val authorName = unescape(parts[3]).trim()
                        val rating = unescape(parts[4]).trim().toIntOrNull() ?: return@mapNotNull null
                        val text = unescape(parts[5])
                        val createdAt = unescape(parts[6]).trim().toLongOrNull() ?: return@mapNotNull null
                        val updatedAt = unescape(parts[7]).trim().toLongOrNull() ?: return@mapNotNull null

                        val type = ReviewTargetType.entries.firstOrNull { it.name == typeStr } ?: return@mapNotNull null
                        if (id.isBlank() || targetId.isBlank() || authorName.isBlank()) return@mapNotNull null
                        if (rating !in 1..5) return@mapNotNull null

                        Review(
                            id = id,
                            targetType = type,
                            targetId = targetId,
                            authorName = authorName,
                            rating = rating,
                            text = text,
                            createdAtMs = createdAt,
                            updatedAtMs = updatedAt
                        )
                    } catch (_: Throwable) {
                        null
                    }
                }
                .toList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    // PUBLIC_INTERFACE
    fun encodeAggregates(aggregates: Map<ReviewTarget, RatingAggregate>): String {
        /** Encode aggregates map to a string for quick-load display. */
        return aggregates.entries.joinToString(separator = LINE_SEP.toString()) { (target, agg) ->
            listOf(
                target.type.name,
                target.id,
                agg.average.toString(),
                agg.count.toString()
            ).joinToString(separator = FIELD_SEP.toString()) { escape(it) }
        }
    }

    // PUBLIC_INTERFACE
    fun decodeAggregates(encoded: String?): Map<ReviewTarget, RatingAggregate> {
        /** Decode persisted aggregates string; returns empty map on missing/malformed data. */
        if (encoded.isNullOrBlank()) return emptyMap()
        return try {
            val out = LinkedHashMap<ReviewTarget, RatingAggregate>()
            encoded.split(LINE_SEP)
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { line ->
                    try {
                        val parts = splitEscaped(line, FIELD_SEP)
                        if (parts.size < 4) return@forEach
                        val typeStr = unescape(parts[0]).trim()
                        val targetId = unescape(parts[1]).trim()
                        val avg = unescape(parts[2]).trim().toDoubleOrNull() ?: return@forEach
                        val count = unescape(parts[3]).trim().toIntOrNull() ?: return@forEach

                        val type = ReviewTargetType.entries.firstOrNull { it.name == typeStr } ?: return@forEach
                        if (targetId.isBlank() || count < 0) return@forEach

                        out[ReviewTarget(type, targetId)] = RatingAggregate(
                            average = avg.coerceIn(0.0, 5.0),
                            count = count
                        )
                    } catch (_: Throwable) {
                        // ignore malformed line
                    }
                }
            out
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    private fun escape(raw: String): String {
        val sb = StringBuilder()
        raw.forEach { ch ->
            when (ch) {
                ESC, FIELD_SEP, LINE_SEP -> {
                    sb.append(ESC)
                    sb.append(ch)
                }
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun unescape(raw: String): String {
        val sb = StringBuilder()
        var escaping = false
        raw.forEach { ch ->
            if (escaping) {
                sb.append(ch)
                escaping = false
            } else {
                if (ch == ESC) escaping = true else sb.append(ch)
            }
        }
        if (escaping) sb.append(ESC)
        return sb.toString()
    }

    private fun splitEscaped(input: String, sep: Char): List<String> {
        val result = ArrayList<String>()
        val current = StringBuilder()
        var escaping = false

        input.forEach { ch ->
            if (escaping) {
                current.append(ch)
                escaping = false
                return@forEach
            }
            when (ch) {
                ESC -> escaping = true
                sep -> {
                    result.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }
}
