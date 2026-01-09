package org.example.app.data.storage

import org.example.app.data.storage.models.StoredCartLine

/**
 * Encoding used:
 * - String sets: pipe-delimited with escaping
 * - Cart lines: newline-delimited "itemId|restaurantId|categoryId|name|description|priceCents|isVeg|quantity"
 *
 * NOTE: This is not a general-purpose serializer, just a lightweight stable format.
 */
internal object SafeCodec {

    private const val FIELD_SEP = '|'
    private const val LINE_SEP = '\n'
    private const val ESC = '\\'

    fun encodeStringSet(values: Set<String>): String {
        return values.joinToString(separator = FIELD_SEP.toString()) { escape(it) }
    }

    fun decodeStringSet(encoded: String?): Set<String> {
        if (encoded.isNullOrBlank()) return emptySet()
        return try {
            splitEscaped(encoded, FIELD_SEP).mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } }.toSet()
        } catch (_: Throwable) {
            emptySet()
        }
    }

    fun encodeCartLines(lines: List<StoredCartLine>): String {
        return lines.joinToString(separator = LINE_SEP.toString()) { l ->
            listOf(
                l.itemId,
                l.restaurantId,
                l.categoryId,
                l.name,
                l.description,
                l.priceCents.toString(),
                l.isVeg.toString(),
                l.quantity.toString()
            ).joinToString(separator = FIELD_SEP.toString()) { escape(it) }
        }
    }

    fun decodeCartLines(encoded: String?): List<StoredCartLine> {
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

                        val itemId = unescape(parts[0])
                        val restaurantId = unescape(parts[1])
                        val categoryId = unescape(parts[2])
                        val name = unescape(parts[3])
                        val description = unescape(parts[4])
                        val priceCents = unescape(parts[5]).toIntOrNull() ?: return@mapNotNull null
                        val isVeg = unescape(parts[6]).toBooleanStrictOrNull() ?: return@mapNotNull null
                        val quantity = unescape(parts[7]).toIntOrNull() ?: return@mapNotNull null

                        if (itemId.isBlank() || quantity <= 0) return@mapNotNull null

                        StoredCartLine(
                            itemId = itemId,
                            restaurantId = restaurantId,
                            categoryId = categoryId,
                            name = name,
                            description = description,
                            priceCents = priceCents,
                            isVeg = isVeg,
                            quantity = quantity
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
        if (escaping) {
            // trailing escape: treat it as literal backslash
            sb.append(ESC)
        }
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

    private fun String.toBooleanStrictOrNull(): Boolean? {
        return when (this.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }
}
