package org.example.app.data.delivery

/**
 * Encoding format (v1):
 * id|restaurantName|itemsSummary|createdAtMs|currentStage|nextTransitionAtMs|stageTimeline
 *
 * stageTimeline = comma-delimited "STAGE=timestampMs" entries, e.g.:
 * PLACED=1700000000000,ACCEPTED=1700000010000
 *
 * Escaping uses a very small custom escaping compatible with SafeCodec behavior.
 */
internal object DeliveryCodec {
    private const val FIELD_SEP = '|'
    private const val KV_SEP = '='
    private const val LIST_SEP = ','
    private const val ESC = '\\'

    fun encode(order: StoredDeliveryOrder): String {
        val stageTimeline = order.stageTimestampsMs.entries
            .sortedBy { it.key.ordinal }
            .joinToString(separator = LIST_SEP.toString()) { (stage, ts) ->
                escape(stage.name) + KV_SEP + escape(ts.toString())
            }

        return listOf(
            order.id,
            order.restaurantName,
            order.itemsSummary,
            order.createdAtMs.toString(),
            order.currentStage.name,
            (order.nextTransitionAtMs ?: -1L).toString(),
            stageTimeline
        ).joinToString(separator = FIELD_SEP.toString()) { escape(it) }
    }

    fun decode(encoded: String?): StoredDeliveryOrder? {
        if (encoded.isNullOrBlank()) return null
        return try {
            val parts = splitEscaped(encoded, FIELD_SEP).map { unescape(it) }
            if (parts.size < 7) return null

            val id = parts[0].trim()
            val restaurant = parts[1]
            val itemsSummary = parts[2]
            val createdAt = parts[3].toLongOrNull() ?: return null
            val stage = DeliveryStage.entries.firstOrNull { it.name == parts[4].trim() } ?: return null
            val nextAtRaw = parts[5].toLongOrNull() ?: return null
            val nextAt = nextAtRaw.takeIf { it > 0L }
            val timelineRaw = parts[6]

            val timeline = decodeStageTimeline(timelineRaw)

            if (id.isBlank()) return null

            StoredDeliveryOrder(
                id = id,
                restaurantName = restaurant,
                itemsSummary = itemsSummary,
                createdAtMs = createdAt,
                currentStage = stage,
                stageTimestampsMs = timeline,
                nextTransitionAtMs = nextAt
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun decodeStageTimeline(raw: String): Map<DeliveryStage, Long> {
        if (raw.isBlank()) return emptyMap()
        return try {
            raw.split(LIST_SEP)
                .mapNotNull { token ->
                    val trimmed = token.trim()
                    if (trimmed.isBlank()) return@mapNotNull null
                    val idx = indexOfUnescaped(trimmed, KV_SEP)
                    if (idx <= 0 || idx >= trimmed.length - 1) return@mapNotNull null
                    val stageStr = unescape(trimmed.substring(0, idx))
                    val tsStr = unescape(trimmed.substring(idx + 1))
                    val stage = DeliveryStage.entries.firstOrNull { it.name == stageStr } ?: return@mapNotNull null
                    val ts = tsStr.toLongOrNull() ?: return@mapNotNull null
                    stage to ts
                }
                .toMap()
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    private fun escape(raw: String): String {
        val sb = StringBuilder()
        raw.forEach { ch ->
            when (ch) {
                ESC, FIELD_SEP, LIST_SEP, KV_SEP -> {
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

    private fun indexOfUnescaped(input: String, target: Char): Int {
        var escaping = false
        input.forEachIndexed { idx, ch ->
            if (escaping) {
                escaping = false
                return@forEachIndexed
            }
            if (ch == ESC) {
                escaping = true
                return@forEachIndexed
            }
            if (ch == target) return idx
        }
        return -1
    }
}
