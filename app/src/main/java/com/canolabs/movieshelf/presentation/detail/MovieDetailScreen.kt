package com.canolabs.movieshelf.presentation.detail

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canolabs.movieshelf.R
import com.canolabs.movieshelf.domain.model.DataError
import com.canolabs.movieshelf.domain.model.Movie
import com.canolabs.movieshelf.domain.model.MovieDetails
import com.canolabs.movieshelf.presentation.common.formatCount
import com.canolabs.movieshelf.presentation.common.formatRating
import com.canolabs.movieshelf.presentation.common.formatReleaseDate
import com.canolabs.movieshelf.presentation.common.formatRuntime
import com.canolabs.movieshelf.presentation.common.messageRes
import com.canolabs.movieshelf.presentation.components.BackdropImage
import com.canolabs.movieshelf.presentation.components.POSTER_ASPECT_RATIO
import com.canolabs.movieshelf.presentation.components.PosterImage
import com.canolabs.movieshelf.presentation.components.StateMessage
import com.canolabs.movieshelf.presentation.components.sharedPoster
import com.canolabs.movieshelf.ui.theme.AppTheme

@Composable
fun MovieDetailRoute(
    viewModel: MovieDetailViewModel,
    posterUrlHint: String?,
    onBack: () -> Unit,
) {
    // Observing the state starts the one-time details fetch in the ViewModel.
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MovieDetailScreen(state = state, posterUrlHint = posterUrlHint, onBack = onBack, onRetry = viewModel::retry)
}

@Composable
fun MovieDetailScreen(
    state: MovieDetailUiState,
    posterUrlHint: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Box {
            when {
                // While the cache is read, the header is laid out from the navigation hint so the
                // shared poster has a target from the first frame of the transition.
                state.details != null || state.isLoading -> DetailContent(
                    state = state,
                    posterUrlHint = posterUrlHint,
                    onRetry = onRetry,
                )
                state.isMissing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    StateMessage(
                        icon = Icons.Rounded.CloudOff,
                        title = stringResource(R.string.detail_missing_title),
                        message = stringResource(
                            state.refreshError?.messageRes() ?: R.string.detail_missing_message,
                        ),
                        action = {
                            FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                        },
                    )
                }
                state.isRefreshing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            FilledTonalIconButton(
                onClick = onBack,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                ),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    state: MovieDetailUiState,
    posterUrlHint: String?,
    onRetry: () -> Unit,
) {
    val details = state.details
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Backdrop(url = details?.backdropUrl)
        Column(
            Modifier
                .offset(y = (-112).dp)
                .padding(horizontal = 20.dp)
                .widthIn(max = 840.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                PosterImage(
                    url = details?.movie?.posterUrl ?: posterUrlHint,
                    contentDescription = details?.let { stringResource(R.string.poster_description, it.movie.title) },
                    modifier = Modifier
                        .sharedPoster(state.movieId)
                        .width(136.dp)
                        .aspectRatio(POSTER_ASPECT_RATIO),
                )
                if (details == null) return@Row
                val movie = details.movie
                val extras = details.extras
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = listOfNotNull(
                            formatReleaseDate(movie.releaseDate) ?: stringResource(R.string.detail_release_unknown),
                            formatRuntime(extras?.runtimeMinutes),
                        ).joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (details == null) return@Column
            val movie = details.movie
            val extras = details.extras

            Spacer(Modifier.height(20.dp))
            RatingRow(movie)

            if (!extras?.tagline.isNullOrBlank()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = extras.tagline,
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!extras?.genres.isNullOrEmpty()) {
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    extras.genres.forEach { GenreLabel(it) }
                }
            }

            AnimatedVisibility(
                visible = state.isRefreshing && extras == null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 20.dp))
            }
            AnimatedVisibility(
                visible = state.extrasUnavailable,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                ExtrasUnavailable(error = state.refreshError, onRetry = onRetry)
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.detail_overview),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = movie.overview ?: stringResource(R.string.detail_no_overview),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            ViewOnTmdbButton(movieId = movie.id)
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

/** Opens the movie's full TMDB page in the external browser; reports when nothing can open it. */
@Composable
private fun ViewOnTmdbButton(movieId: Long) {
    val context = LocalContext.current
    val noAppMessage = stringResource(R.string.detail_no_browser)
    OutlinedButton(
        onClick = {
            if (!context.openTmdbMoviePage(movieId)) {
                Toast.makeText(context, noAppMessage, Toast.LENGTH_SHORT).show()
            }
        },
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.detail_view_on_tmdb))
    }
}

@Composable
private fun Backdrop(url: String?) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        BackdropImage(url = url, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to surface)),
        )
    }
}

@Composable
private fun RatingRow(movie: Movie) {
    val rating = formatRating(movie.voteAverage, movie.voteCount)
    if (rating == null) {
        Text(
            text = stringResource(R.string.detail_not_rated),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val votes = movie.voteCount?.let(::formatCount)
    val description = stringResource(R.string.rating_description, rating)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Icon(
            Icons.Rounded.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(text = rating, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.detail_out_of_ten),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (votes != null) {
            Text(
                text = "  ·  " + stringResource(R.string.detail_rating_votes, votes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GenreLabel(name: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ExtrasUnavailable(error: DataError?, onRetry: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp),
    ) {
        Icon(
            Icons.Rounded.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(
                if (error == DataError.Network || error == DataError.Timeout) {
                    R.string.detail_extras_unavailable
                } else {
                    R.string.error_generic_title
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun MovieDetailPreview() {
    AppTheme(dynamicColor = false) {
        MovieDetailScreen(
            state = MovieDetailUiState(
                movieId = 78,
                isLoading = false,
                details = MovieDetails(
                    movie = Movie(
                        id = 78,
                        title = "Blade Runner",
                        overview = "In the smog-choked dystopian Los Angeles of 2019, blade runner Rick Deckard is " +
                            "called out of retirement to terminate a quartet of replicants.",
                        releaseDate = "1982-06-25",
                        posterUrl = null,
                        voteAverage = 7.9,
                        voteCount = 14_000,
                    ),
                    backdropUrl = null,
                    extras = MovieDetails.Extras(117, listOf("Science Fiction", "Drama", "Thriller"), "Man has made his match… now it’s his problem."),
                ),
            ),
            posterUrlHint = null,
            onBack = {},
            onRetry = {},
        )
    }
}
