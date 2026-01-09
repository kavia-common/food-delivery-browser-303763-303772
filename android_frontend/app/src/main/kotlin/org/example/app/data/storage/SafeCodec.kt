package org.example.app.data.storage

import org.example.app.data.cart.AppliedPromo
import org.example.app.data.cart.FeeSettings
import org.example.app.data.cart.PromoKind
import org.example.app.data.storage.models.StoredCartLine

/**
 * Encoding used:
 * - String sets: pipe-delimited with escaping
 * - Cart lines (v2): newline-delimited
 *   "itemId|restaurantId|categoryId|name|description|priceCents|isVeg|quantity|configurationKey|selectedVariantOptionIds|selectedAddOnOptionIds|itemNote"
 * - Cart lines (v1 legacy): newline-delimited
 *   "itemId|restaurantId|categoryId|name|description|priceCents|isVeg|quantity"
 * - Applied promo: "code|kind|value"
 * - Fee settings: "deliveryFeeCents|serviceFeeCents|taxRate"
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
                l.quantity.toString(),
                l.configurationKey,
                l.selectedVariantOptionIds,
                l.selectedAddOnOptionIds,
                l.itemNote
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

                        // Backward compatible optional fields (v2+).
                        val configurationKey = if (parts.size >= 9) unescape(parts[8]) else ""
                        val selectedVariantOptionIds = if (parts.size >= 10) unescape(parts[9]) else ""
                        val selectedAddOnOptionIds = if (parts.size >= 11) unescape(parts[10]) else ""
                        val itemNote = if (parts.size >= 12) unescape(parts[11]) else ""

                        StoredCartLine(
                            itemId = itemId,
                            restaurantId = restaurantId,
                            categoryId = categoryId,
                            name = name,
                            description = description,
                            priceCents = priceCents,
                            isVeg = isVeg,
                            quantity = quantity,
                            configurationKey = configurationKey,
                            selectedVariantOptionIds = selectedVariantOptionIds,
                            selectedAddOnOptionIds = selectedAddOnOptionIds,
                            itemNote = itemNote
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

    fun encodeAppliedPromo(promo: AppliedPromo): String {
        return listOf(
            promo.code,
            promo.kind.name,
            promo.value.toString()
        ).joinToString(separator = FIELD_SEP.toString()) { escape(it) }
    }

    fun decodeAppliedPromo(encoded: String?): AppliedPromo? {
        if (encoded.isNullOrBlank()) return null
        return try {
            val parts = splitEscaped(encoded, FIELD_SEP)
            if (parts.size < 3) return null

            val code = unescape(parts[0]).trim()
            val kindStr = unescape(parts[1]).trim()
            val value = unescape(parts[2]).trim().toIntOrNull() ?: return null

            val kind = PromoKind.entries.firstOrNull { it.name == kindStr } ?: return null
            if (code.isBlank()) return null

            AppliedPromo(code = code, kind = kind, value = value)
        } catch (_: Throwable) {
            null
        }
    }

    fun encodeFeeSettings(settings: FeeSettings): String {
        // Keep tax rate as a string; parse defensively.
        return listOf(
            settings.deliveryFeeCents.toString(),
            settings.serviceFeeCents.toString(),
            settings.taxRate.toString()
        ).joinToString(separator = FIELD_SEP.toString()) { escape(it) }
    }

    fun decodeFeeSettings(encoded: String?): FeeSettings? {
        if (encoded.isNullOrBlank()) return null
        return try {
            val parts = splitEscaped(encoded, FIELD_SEP)
            if (parts.size < 3) return null

            val deliveryFee = unescape(parts[0]).toIntOrNull() ?: return null
            val serviceFee = unescape(parts[1]).toIntOrNull() ?: return null
            val taxRate = unescape(parts[2]).toDoubleOrNull() ?: return null

            // Basic sanity constraints.
            if (deliveryFee < 0 || serviceFee < 0 || taxRate < 0.0) return null

            FeeSettings(
                deliveryFeeCents = deliveryFee,
                serviceFeeCents = serviceFee,
                taxRate = taxRate
            )
        } catch (_: Throwable) {
            null
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
