package com.canolabs.movieshelf.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Movie
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canolabs.movieshelf.R
import com.canolabs.movieshelf.presentation.components.SearchBarPill
import com.canolabs.movieshelf.presentation.settings.SettingsSheet
import com.canolabs.movieshelf.presentation.settings.SettingsViewModel
import com.canolabs.movieshelf.ui.theme.AppTheme

/**
 * Initial destination: brand header with settings, the search bar, and Discover. It holds no data of
 * its own; the only state is the settings sheet's visibility and the settings it shows.
 */
@Composable
fun SearchRoute(
    settingsViewModel: SettingsViewModel,
    onSearchClick: () -> Unit,
    onDiscoverClick: () -> Unit,
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    SearchScreen(
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
fun SearchScreen(
    onSearchClick: () -> Unit,
    onDiscoverClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 720.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, bottom = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() },
                    )
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }
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
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(88.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Movie, contentDescription = null, modifier = Modifier.size(40.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.search_headline),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.search_supporting),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 420.dp),
                )
                Spacer(Modifier.height(32.dp))
                DiscoverCard(onClick = onDiscoverClick, modifier = Modifier.widthIn(max = 420.dp))
            }
            Text(
                text = stringResource(R.string.search_tmdb_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

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
private fun SearchScreenPreview() {
    AppTheme(dynamicColor = false) { SearchScreen(onSearchClick = {}, onDiscoverClick = {}, onSettingsClick = {}) }
}
