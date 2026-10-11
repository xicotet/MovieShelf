package com.canolabs.movieshelf.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.canolabs.movieshelf.R
import com.canolabs.movieshelf.presentation.components.SearchBarPill
import com.canolabs.movieshelf.presentation.settings.SettingsSheet
import com.canolabs.movieshelf.presentation.settings.SettingsViewModel
import com.canolabs.movieshelf.ui.theme.AppTheme

/**
 * Initial destination: the search bar (with settings), a short introduction and Discover. It holds no
 * data of its own; the only state is the settings sheet's visibility and the settings it shows.
 */
@Composable
fun HomeRoute(
    settingsViewModel: SettingsViewModel,
    onSearchClick: () -> Unit,
    onDiscoverClick: () -> Unit,
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    HomeScreen(
        onSearchClick = onSearchClick,
        onDiscoverClick = onDiscoverClick,
        onSettingsClick = { showSettings = true },
    )
    val current = settings
    if (showSettings && current != null) {
        SettingsSheet(
            settings = current,
            onThemeModeSelected = settingsViewModel::onThemeModeSelected,
            onTextSizeSelected = settingsViewModel::onTextSizeSelected,
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
fun HomeScreen(
    onSearchClick: () -> Unit,
    onDiscoverClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The gradient is drawn edge to edge, behind the system bars; only the content is inset.
    Box(
        modifier
            .fillMaxSize()
            .homeGradient(),
    ) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SearchBarPill(
                text = stringResource(R.string.search_placeholder),
                isPlaceholder = true,
                onClick = onSearchClick,
                clickLabel = stringResource(R.string.search_placeholder),
                leading = {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    }
                },
                trailing = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
                modifier = Modifier.widthIn(max = 720.dp),
            )
            // The middle area centers its content and scrolls when it does not fit (e.g. Large text).
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    FilmReelsAnimation(Modifier.widthIn(max = 280.dp).fillMaxWidth())
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.home_headline),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.home_supporting),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 420.dp),
                    )
                }
            }
            DiscoverCard(
                onClick = onDiscoverClick,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .padding(top = 8.dp, bottom = 8.dp),
            )
        }
    }
}

/**
 * Soft glows of the theme's container colors over the surface: primary at the top, secondary on the
 * side and tertiary at the bottom. Built from [MaterialTheme.colorScheme], so it follows light and
 * dark mode. Each glow fades to its own color at zero alpha (not to transparent black), so the
 * edges never darken.
 */
@Composable
private fun Modifier.homeGradient(): Modifier {
    val colors = MaterialTheme.colorScheme
    return drawBehind {
        drawRect(colors.surface)
        drawGlow(colors.primaryContainer, alpha = 0.9f, x = 0.15f, y = 0.0f, radius = 0.75f)
        drawGlow(colors.secondaryContainer, alpha = 0.45f, x = 1.0f, y = 0.45f, radius = 0.55f)
        drawGlow(colors.tertiaryContainer, alpha = 0.7f, x = 0.2f, y = 1.0f, radius = 0.7f)
    }
}

private fun DrawScope.drawGlow(
    color: Color,
    alpha: Float,
    x: Float,
    y: Float,
    radius: Float,
) {
    drawRect(
        Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = Offset(size.width * x, size.height * y),
            radius = size.maxDimension * radius,
        ),
    )
}

/**
 * Decorative film reels (res/raw/home_film_reels.json, an original animation). Its colors are
 * replaced at runtime with the current Material theme, so it matches light and dark mode; the reel
 * holes are real cut-outs, so the background gradient shows through them.
 */
@Composable
private fun FilmReelsAnimation(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.home_film_reels))
    val colors = MaterialTheme.colorScheme
    val dynamicProperties = rememberLottieDynamicProperties(
        themeColor("Reel Fill", colors.primary),
        themeColor("Hub Fill", colors.primaryContainer),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = colors.secondary.toArgb(),
            keyPath = arrayOf("**", "Film Stroke"),
        ),
    )
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        dynamicProperties = dynamicProperties,
        modifier = modifier.aspectRatio(360f / 220f),
    )
}

@Composable
private fun themeColor(fillName: String, color: Color) = rememberLottieDynamicProperty(
    property = LottieProperty.COLOR,
    value = color.toArgb(),
    keyPath = arrayOf("**", fillName),
)

/** Entry point to TMDB's popular movies, for when the user has no title in mind. */
@Composable
private fun DiscoverCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Icon(Icons.Outlined.Explore, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = stringResource(R.string.discover_title), style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(R.string.discover_subtitle), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AppTheme(dynamicColor = false) { HomeScreen(onSearchClick = {}, onDiscoverClick = {}, onSettingsClick = {}) }
}
