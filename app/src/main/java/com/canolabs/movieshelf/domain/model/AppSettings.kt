package com.canolabs.movieshelf.domain.model

/** User preferences, persisted on the device. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val textSize: TextSize = TextSize.Default,
)

enum class ThemeMode { System, Light, Dark }

enum class TextSize { Small, Default, Large }
