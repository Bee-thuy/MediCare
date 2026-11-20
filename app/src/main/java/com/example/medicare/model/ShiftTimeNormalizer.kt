package com.example.medicare.model

import java.time.LocalTime
import java.text.Normalizer

/** Reads old afternoon shifts saved with 12-hour clock values as 24-hour times. */
object ShiftTimeNormalizer {
    fun isAfternoonName(name: String): Boolean = Normalizer.normalize(
        name.lowercase(), Normalizer.Form.NFD,
    ).replace(Regex("\\p{M}+"), "").contains("chieu")

    fun normalize(name: String, start: String, end: String): Pair<String, String> {
        val from = runCatching { LocalTime.parse(start) }.getOrNull() ?: return start to end
        val until = runCatching { LocalTime.parse(end) }.getOrNull() ?: return start to end
        if (isAfternoonName(name) && from.hour in 1..5 &&
            until.hour in 2..6 && until.isAfter(from)) {
            return from.plusHours(12).toString() to until.plusHours(12).toString()
        }
        return start to end
    }
}
