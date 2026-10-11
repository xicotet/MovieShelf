package com.canolabs.movieshelf.presentation.common

import java.text.DateFormat
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * The app ships English text only, so dates and numbers are formatted in English too, whatever the
 * device language is (otherwise a Spanish device would show e.g. "hace 9 minutos" or "7,9").
 */
private val AppLocale: Locale = Locale.US

/** Formats an API date (`yyyy-MM-dd`) as e.g. "Jun 25, 1982", falling back to the raw value if unparsable. */
fun formatReleaseDate(isoDate: String?): String? {
    if (isoDate.isNullOrBlank()) return null
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(isoDate)
        parsed?.let { DateFormat.getDateInstance(DateFormat.MEDIUM, AppLocale).format(it) } ?: isoDate
    } catch (_: ParseException) {
        isoDate
    }
}

fun releaseYear(isoDate: String?): String? =
    isoDate?.takeIf { it.length >= 4 && it.take(4).all(Char::isDigit) }?.take(4)

/** TMDB ratings are 0–10; `null` or 0 votes means "not rated yet". */
fun formatRating(voteAverage: Double?, voteCount: Int?): String? {
    if (voteAverage == null || voteAverage <= 0.0 || voteCount == 0) return null
    return String.format(AppLocale, "%.1f", voteAverage)
}

/** Whole numbers with English grouping, e.g. "15,502". */
fun formatCount(count: Int): String = NumberFormat.getIntegerInstance(AppLocale).format(count)

fun formatRuntime(minutes: Int?): String? {
    if (minutes == null || minutes <= 0) return null
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}

/**
 * English relative time for "Updated …": "just now", "5 minutes ago", "2 hours ago", "3 days ago",
 * then an absolute date such as "Oct 4, 2026". Times in the future (clock changes) read "just now".
 */
fun formatRelativeTime(timestampMillis: Long, nowMillis: Long): String {
    val elapsed = nowMillis - timestampMillis
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
    val days = TimeUnit.MILLISECONDS.toDays(elapsed)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> plural(minutes, "minute") + " ago"
        hours < 24 -> plural(hours, "hour") + " ago"
        days < 7 -> plural(days, "day") + " ago"
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM, AppLocale).format(Date(timestampMillis))
    }
}

private fun plural(count: Long, unit: String) = if (count == 1L) "1 $unit" else "$count ${unit}s"
