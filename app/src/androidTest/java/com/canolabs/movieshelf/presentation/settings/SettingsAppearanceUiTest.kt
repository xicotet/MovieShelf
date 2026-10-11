package com.canolabs.movieshelf.presentation.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.canolabs.movieshelf.domain.model.TextSize
import com.canolabs.movieshelf.domain.model.ThemeMode
import com.canolabs.movieshelf.ui.theme.AppTheme
import com.canolabs.movieshelf.ui.theme.backgroundDark
import com.canolabs.movieshelf.ui.theme.backgroundLight
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The stored preferences reach the composition: generated color scheme and font scale. */
@RunWith(AndroidJUnit4::class)
class SettingsAppearanceUiTest {

    @get:Rule
    val compose = createComposeRule()

    private fun backgroundFor(mode: ThemeMode): Color {
        var background = Color.Unspecified
        compose.setContent {
            AppTheme(darkTheme = mode.isDark(isSystemInDarkTheme()), dynamicColor = false) {
                background = MaterialTheme.colorScheme.background
            }
        }
        compose.waitForIdle()
        return background
    }

    @Test
    fun darkModeUsesTheGeneratedDarkScheme() {
        assertEquals(backgroundDark, backgroundFor(ThemeMode.Dark))
    }

    @Test
    fun lightModeUsesTheGeneratedLightScheme() {
        assertEquals(backgroundLight, backgroundFor(ThemeMode.Light))
    }

    @Test
    fun textSizeScalesTheDeviceFontScale() {
        var deviceScale = 0f
        var appliedScale = 0f
        compose.setContent {
            deviceScale = LocalDensity.current.fontScale
            ProvideTextSize(TextSize.Large) {
                appliedScale = LocalDensity.current.fontScale
            }
        }
        compose.waitForIdle()

        assertEquals(deviceScale * TextSize.Large.fontScale, appliedScale, 0.0001f)
    }
}
