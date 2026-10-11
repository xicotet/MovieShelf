package com.canolabs.movieshelf.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.canolabs.movieshelf.R

val provider: GoogleFont.Provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Downloadable fonts are resolved at runtime through Google Play services. Each weight used by the
// type scale is declared explicitly so text is not rendered with synthesized bold.
private val inter = GoogleFont("Inter")
private val spaceGrotesk = GoogleFont("Space Grotesk")

val bodyFontFamily = FontFamily(
    Font(googleFont = inter, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = inter, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = inter, fontProvider = provider, weight = FontWeight.SemiBold),
)

val displayFontFamily = FontFamily(
    Font(googleFont = spaceGrotesk, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = spaceGrotesk, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = spaceGrotesk, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = spaceGrotesk, fontProvider = provider, weight = FontWeight.Bold),
)

// Default Material 3 typography values
val baseline = Typography()

val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = displayFontFamily),
    displayMedium = baseline.displayMedium.copy(fontFamily = displayFontFamily),
    displaySmall = baseline.displaySmall.copy(fontFamily = displayFontFamily),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = displayFontFamily),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFontFamily),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = displayFontFamily),
    titleLarge = baseline.titleLarge.copy(fontFamily = displayFontFamily),
    titleMedium = baseline.titleMedium.copy(fontFamily = displayFontFamily),
    titleSmall = baseline.titleSmall.copy(fontFamily = displayFontFamily),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily),
    bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily),
    labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily),
    labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily),
    labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily),
)
