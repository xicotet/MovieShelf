package com.canolabs.movieshelf.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade

const val POSTER_ASPECT_RATIO = 2f / 3f

/**
 * Poster with a neutral placeholder while loading and a fallback icon when the movie has no artwork
 * or the image cannot be loaded (e.g. offline and not in Coil's disk cache).
 */
@Composable
fun PosterImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var failed by remember(url) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(PosterShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (url == null || failed) {
            Icon(
                imageVector = Icons.Outlined.Movie,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
        if (url != null && !failed) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    // Results and Detail request the same URL, so the shared-element poster is served
                    // from the memory cache and never flashes a placeholder mid-transition.
                    .placeholderMemoryCacheKey(url)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onState = { state -> if (state is AsyncImagePainter.State.Error) failed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Wide backdrop image without placeholder chrome; renders nothing when unavailable. */
@Composable
fun BackdropImage(url: String?, modifier: Modifier = Modifier) {
    if (url == null) return
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxWidth(),
    )
}
