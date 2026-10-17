package com.driveflow.driver.reader.parser

import com.driveflow.driver.core.model.RoadState
import java.util.Locale

/**
 * Text parsing utilities optimized for inDrive, DiDi, and Uber in Egypt.
 * Handles both Eastern Arabic-Indic numerals (٠١٢٣٤٥٦٧٨٩) and Western digits (0123456789),
 * Arabic decimal marks, currency notations (ج.م, EGP, LE, جنيه), distance strings (كم, km, م, m),
 * rating strings (★ 4.85, 120 trips), and request age labels.
 */
object ArabicTextParser {

    private val ARABIC_INDIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    private val EASTERN_INDIC_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun normalizeDigits(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when {
                ch in '0'..'9' -> sb.append(ch)
                ch in '٠'..'٩' -> sb.append((ch - '٠' + '0'.code).toChar())
                ch in '۰'..'۹' -> sb.append((ch - '۰' + '0'.code).toChar())
                ch == '٫' -> sb.append('.')
                ch == '٬' -> { /* drop thousand separator */ }
                ch == ',' -> sb.append('.')
                else -> sb.append(ch)
            }
        }
        return sb.toString().trim()
    }

    fun extractFirstDouble(input: String?): Double? {
        if (input.isNullOrBlank()) return null
        val normalized = normalizeDigits(input)
        val regex = Regex("""\b\d+(?:\.\d+)?\b""")
        val match = regex.find(normalized) ?: return null
        return match.value.toDoubleOrNull()
    }

    fun extractFirstInt(input: String?): Int? {
        if (input.isNullOrBlank()) return null
        val normalized = normalizeDigits(input)
        val regex = Regex("""\b\d+\b""")
        val match = regex.find(normalized) ?: return null
        return match.value.toIntOrNull()
    }

    fun parseFare(text: String?): Pair<Double?, String?> {
        if (text.isNullOrBlank()) return Pair(null, null)
        val raw = text.trim()
        val normalized = normalizeDigits(raw)

        val currency = when {
            raw.contains("ج.م") || raw.contains("جنيه") || raw.contains("جم") ||
            raw.contains("ج") || raw.contains("EGP", ignoreCase = true) ||
            raw.contains("LE", ignoreCase = true) || raw.contains("L.E", ignoreCase = true) -> "EGP"
            raw.contains("SAR", ignoreCase = true) || raw.contains("ريال") -> "SAR"
            raw.contains("USD", ignoreCase = true) || raw.contains("$") -> "USD"
            else -> "EGP"
        }

        val fareRegex = Regex("""(\d+(?:\.\d+)?)""")
        val match = fareRegex.find(normalized)
        val fare = match?.value?.toDoubleOrNull()
        return Pair(fare, currency)
    }

    fun parseDistanceKm(text: String?): Double? {
        if (text.isNullOrBlank()) return null
        val raw = text.trim()
        val normalized = normalizeDigits(raw)

        val isMeters = (normalized.contains("متر") || normalized.endsWith("م") ||
                normalized.contains(" m", ignoreCase = true) || normalized.endsWith("m", ignoreCase = true)) &&
                !normalized.contains("كم") && !normalized.contains("km", ignoreCase = true)

        val regex = Regex("""(\d+(?:\.\d+)?)""")
        val match = regex.find(normalized) ?: return null
        val value = match.value.toDoubleOrNull() ?: return null
        return if (isMeters) {
            value / 1000.0
        } else {
            value
        }
    }

    fun parseMinutes(text: String?): Int? {
        if (text.isNullOrBlank()) return null
        val normalized = normalizeDigits(text.trim())

        var extraHours = 0
        if (normalized.contains("ساعتان") || normalized.contains("ساعتين")) {
            extraHours = 120
        } else if (normalized.contains("ساعة")) {
            extraHours = 60
        }

        val regex = Regex("""(\d+)\s*(?:د|دقيقة|دقائق|min|mins|m)?""")
        val match = regex.find(normalized)
        val mins = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val total = extraHours + mins
        return if (total > 0) total else null
    }

    fun parseAgeSeconds(text: String?): Int? {
        if (text.isNullOrBlank()) return null
        val raw = text.trim()
        if (raw.contains("الآن") || raw.contains("now", ignoreCase = true)) {
            return 0
        }
        val normalized = normalizeDigits(raw)

        if (normalized.contains("ثانية") || normalized.contains("ثوان") ||
            normalized.contains("ث") || normalized.contains("s", ignoreCase = true)) {
            val sec = extractFirstInt(normalized)
            if (sec != null) return sec
        }

        if (normalized.contains("دقيقة") || normalized.contains("دقائق") ||
            normalized.contains("د") || normalized.contains("m", ignoreCase = true)) {
            val mins = extractFirstInt(normalized)
            if (mins != null) return mins * 60
        }

        return extractFirstInt(normalized)
    }

    fun parseRatingAndTrips(text: String?): Pair<Double?, Int?> {
        if (text.isNullOrBlank()) return Pair(null, null)
        val raw = text.trim()
        if (raw.contains("جديد") || raw.contains("New", ignoreCase = true)) {
            return Pair(5.0, 0)
        }
        val normalized = normalizeDigits(raw)

        val ratingRegex = Regex("""([1-5](?:\.\d{1,2})?)""")
        val ratingMatch = ratingRegex.find(normalized)
        val rating = ratingMatch?.value?.toDoubleOrNull()

        val tripsInParenRegex = Regex("""\(([\d]+)\)""")
        val parenMatch = tripsInParenRegex.find(normalized)
        var trips = parenMatch?.groupValues?.get(1)?.toIntOrNull()

        if (trips == null) {
            val tripsWordRegex = Regex("""([\d]+)\s*(?:رحلة|رحلات|مشوار|مشاوير|trips|rides)""", RegexOption.IGNORE_CASE)
            val tripsMatch = tripsWordRegex.find(normalized)
            trips = tripsMatch?.groupValues?.get(1)?.toIntOrNull()
        }

        return Pair(rating, trips)
    }

    fun parseExtraStops(text: String?): Int {
        if (text.isNullOrBlank()) return 0
        val normalized = normalizeDigits(text.trim())
        val plusMatch = Regex("""\+(\d+)""").find(normalized)
        if (plusMatch != null) {
            return plusMatch.groupValues[1].toIntOrNull() ?: 1
        }
        if (normalized.contains("وقفتان") || normalized.contains("وقفتين")) {
            return 2
        }
        if (normalized.contains("وقفة") || normalized.contains("وقفات") ||
            normalized.contains("stop", ignoreCase = true)) {
            val count = extractFirstInt(normalized)
            return count ?: 1
        }
        return 0
    }

    fun parsePaymentMethod(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val lower = text.trim().lowercase(Locale.ROOT)
        return when {
            lower.contains("نقدي") || lower.contains("كاش") || lower.contains("نقدًا") || lower.contains("cash") -> "CASH"
            lower.contains("محفظة") || lower.contains("wallet") -> "WALLET"
            lower.contains("بطاقة") || lower.contains("فيزا") || lower.contains("card") || lower.contains("visa") -> "CARD"
            else -> text.trim()
        }
    }

    fun parseRoadState(text: String?): RoadState {
        if (text.isNullOrBlank()) return RoadState.UNKNOWN
        val lower = text.trim().lowercase(Locale.ROOT)
        return when {
            lower.contains("زحمة") || lower.contains("مزدحم") || lower.contains("واقف") || lower.contains("شديد") ||
            lower.contains("بطيء جداً") || lower.contains("بطيء جدا") || lower.contains("خانق") ||
            lower.contains("heavy") || lower.contains("hard") || lower.contains("gridlock") -> RoadState.HARD

            lower.contains("متوسط") || lower.contains("بطيء") || lower.contains("تقيل") || lower.contains("متحرك ببطء") ||
            lower.contains("شبه واقف") || lower.contains("كثافة") || lower.contains("شبه زحمة") || lower.contains("متباطئ") ||
            lower.contains("slow") || lower.contains("moderate") -> RoadState.SLOW

            lower.contains("سريع") || lower.contains("سالك") || lower.contains("فاضي") || lower.contains("مفتوح") ||
            lower.contains("easy") || lower.contains("clear") -> RoadState.EASY

            else -> RoadState.UNKNOWN
        }
    }

    fun cleanAddress(address: String?): String? {
        if (address.isNullOrBlank()) return null
        var cleaned = cleanOcrText(address).trim()
        val prefixes = listOf(
            "نقطة الانطلاق:", "مكان الالتقاء:", "من:", "نقطة الركوب:", "من موقع:", "مكان الركوب:",
            "الوجهة:", "إلى:", "الوصول:",
            "Pickup:", "Dropoff:", "Destination:", "From:", "To:"
        )
        for (prefix in prefixes) {
            if (cleaned.startsWith(prefix, ignoreCase = true)) {
                cleaned = cleaned.substring(prefix.length).trim()
            }
        }
        return cleaned.ifBlank { null }
    }

    fun cleanOcrText(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '\u200E', '\u200F', '\u202A', '\u202B', '\u202C', '\u202D', '\u202E',
                '\u2066', '\u2067', '\u2068', '\u2069', '\u200B', '\u200C', '\u200D', '\uFEFF' -> {}
                '\u00A0' -> sb.append(' ')
                else -> sb.append(ch)
            }
        }
        var text = sb.toString()
        text = text.replace(Regex("""(\d+)٫(\d+)"""), "$1.$2")
        text = text.replace(Regex("""(?<=\d)[Oo](?=\b|\d|\.)"""), "0")
        text = text.replace(Regex("""(?<=\b)[Oo](?=\.\d|\d)"""), "0")
        text = text.replace(Regex("""(?<=\d)[lI|](?=\b|\d|\.)"""), "1")
        text = text.replace(Regex("""(?<=\b)[lI|](?=\.\d|\d)"""), "1")
        text = text.replace(Regex("""(\d+)(ج\.م|EGP|LE|جنيه|km|كم)"""), "$1 $2")
        text = text.replace(Regex("""(ج\.م|EGP|LE|جنيه|km|كم)(\d+)"""), "$1 $2")
        return text.trim()
    }

    fun parseProduct(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val cleaned = cleanOcrText(text)
        val lower = cleaned.lowercase(Locale.ROOT)
        return when {
            lower.contains("saver") || lower.contains("سيفر") || lower.contains("سافر") -> {
                if (lower.contains("uber") || lower.contains("أوبر")) "Uber Saver" else "Saver"
            }
            lower.contains("comfort") || lower.contains("كومفورت") -> {
                if (lower.contains("uber") || lower.contains("أوبر")) "Uber Comfort" else "Comfort"
            }
            lower.contains("uberx") || lower.contains("uber x") || lower.contains("أوبر اكس") || lower.contains("اوبر اكس") -> "UberX"
            lower.contains("select") || lower.contains("سيلكت") -> "Uber Select"
            lower.contains("black") || lower.contains("بلاك") -> "Uber Black"
            lower.contains("flex") || lower.contains("فليكس") -> "DiDi Flex"
            lower.contains("واصلني") || lower.contains("waselny") -> "DiDi Waselny"
            lower.contains("إكسبريس") || lower.contains("express") -> "DiDi Express"
            lower.contains("indrive") || lower.contains("إن درايف") -> "inDrive"
            else -> cleaned.take(30).trim()
        }
    }

    fun isUberSaver(product: String?): Boolean {
        if (product.isNullOrBlank()) return false
        val lower = product.lowercase(Locale.ROOT)
        return lower.contains("saver") || lower.contains("سيفر") || lower.contains("سافر")
    }

    fun isValidFare(fare: Double?): Boolean = fare != null && fare in 5.0..5000.0
    fun isValidDistance(km: Double?): Boolean = km != null && km in 0.05..300.0
    fun isValidRating(rating: Double?): Boolean = rating != null && rating in 1.0..5.0
}
