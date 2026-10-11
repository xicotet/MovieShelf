package com.canolabs.movieshelf

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canolabs.movieshelf.app.navigation.AppNavigation
import com.canolabs.movieshelf.presentation.settings.ProvideTextSize
import com.canolabs.movieshelf.presentation.settings.SettingsViewModel
import com.canolabs.movieshelf.presentation.settings.isDark
import com.canolabs.movieshelf.ui.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            // Wait for the stored preferences (one quick local read) instead of drawing a first frame
            // with defaults and then switching theme or text size.
            val current = settings ?: return@setContent
            val darkTheme = current.themeMode.isDark(isSystemInDarkTheme())

            // System bar icons must follow the app's theme, which can differ from the device's.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }

            // Dynamic color is disabled so the Material Theme Builder palette is used on every device.
            AppTheme(darkTheme = darkTheme, dynamicColor = false) {
                ProvideTextSize(current.textSize) {
                    AppNavigation()
                }
            }
        }
    }

    private companion object {
        // Same navigation bar scrims as androidx.activity's enableEdgeToEdge defaults.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
