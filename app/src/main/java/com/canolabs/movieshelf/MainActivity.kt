package com.canolabs.movieshelf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.canolabs.movieshelf.app.navigation.AppNavigation
import com.canolabs.movieshelf.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Dynamic color is disabled so the Material Theme Builder palette is used on every device.
            AppTheme(dynamicColor = false) {
                AppNavigation()
            }
        }
    }
}
