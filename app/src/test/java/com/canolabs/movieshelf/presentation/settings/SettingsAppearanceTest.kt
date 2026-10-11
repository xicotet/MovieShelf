package com.canolabs.movieshelf.presentation.settings

import com.canolabs.movieshelf.domain.model.TextSize
import com.canolabs.movieshelf.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsAppearanceTest {

    @Test
    fun `system follows the device while light and dark are forced`() {
        assertTrue(ThemeMode.System.isDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.System.isDark(systemInDarkTheme = false))
        assertFalse(ThemeMode.Light.isDark(systemInDarkTheme = true))
        assertTrue(ThemeMode.Dark.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `default text size keeps the device scale and the others step around it`() {
        assertEquals(1f, TextSize.Default.fontScale)
        assertTrue(TextSize.Small.fontScale < 1f)
        assertTrue(TextSize.Large.fontScale > 1f)
    }
}
