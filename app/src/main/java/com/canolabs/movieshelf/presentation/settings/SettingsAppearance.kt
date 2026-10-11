package com.canolabs.movieshelf.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.canolabs.movieshelf.domain.model.TextSize
import com.canolabs.movieshelf.domain.model.ThemeMode

/** System follows the device; Light and Dark force the app's own generated color schemes. */
fun ThemeMode.isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
    ThemeMode.System -> systemInDarkTheme
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

/** Multiplier applied on top of the device's own font scale. */
val TextSize.fontScale: Float
    get() = when (this) {
        TextSize.Small -> 0.9f
        TextSize.Default -> 1f
        TextSize.Large -> 1.15f
    }

/**
 * Scales every `sp` text size (all Material typography and components) by [textSize]'s factor,
 * relative to the device's font scale. `dp` dimensions, and therefore the layout, are unchanged.
 */
@Composable
fun ProvideTextSize(textSize: TextSize, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * textSize.fontScale),
        content = content,
    )
}
