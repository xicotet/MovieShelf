package com.canolabs.movieshelf.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canolabs.movieshelf.R
import com.canolabs.movieshelf.presentation.components.SearchBarPill
import com.canolabs.movieshelf.ui.theme.AppTheme

/**
 * Initial destination. It holds no data of its own, so it has no ViewModel: activating the search bar
 * opens the full-screen Search Entry destination.
 */
@Composable
fun SearchScreen(
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Box(Modifier.widthIn(max = 720.dp).align(Alignment.CenterHorizontally)) {
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
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(96.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Movie, contentDescription = null, modifier = Modifier.size(44.dp))
                    }
                }
                Spacer(Modifier.height(28.dp))
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
            }
            Text(
                text = stringResource(R.string.search_tmdb_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    AppTheme(dynamicColor = false) { SearchScreen(onSearchClick = {}) }
}
