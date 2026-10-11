package com.canolabs.movieshelf.presentation.common

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/** Runs with a Spanish default locale to prove the output is English regardless of the device language. */
class FormattersTest {

    private lateinit var originalLocale: Locale
    private val minute = 60_000L
    private val now = 1_800_000_000_000L

    @Before
    fun useSpanishDeviceLocale() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
    }

    @After
    fun restoreLocale() = Locale.setDefault(originalLocale)

    @Test
    fun `release dates are english`() {
        assertEquals("Jun 25, 1982", formatReleaseDate("1982-06-25"))
        assertEquals("not-a-date", formatReleaseDate("not-a-date"))
    }

    @Test
    fun `ratings use a decimal point`() {
        assertEquals("7.9", formatRating(7.9, 100))
    }

    @Test
    fun `counts use english grouping`() {
        assertEquals("15,502", formatCount(15_502))
    }

    @Test
    fun `relative time is english`() {
        assertEquals("just now", formatRelativeTime(now - 30_000, now))
        assertEquals("1 minute ago", formatRelativeTime(now - minute, now))
        assertEquals("9 minutes ago", formatRelativeTime(now - 9 * minute, now))
        assertEquals("2 hours ago", formatRelativeTime(now - 120 * minute, now))
        assertEquals("1 day ago", formatRelativeTime(now - 24 * 60 * minute, now))
        assertEquals("just now", formatRelativeTime(now + minute, now))
    }

    @Test
    fun `older updates show an english date`() {
        val tenDaysAgo = now - 10 * 24 * 60 * minute
        val expected = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.US)
            .format(java.util.Date(tenDaysAgo))
        assertEquals(expected, formatRelativeTime(tenDaysAgo, now))
    }
}
