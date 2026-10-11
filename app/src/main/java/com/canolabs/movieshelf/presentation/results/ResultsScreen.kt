package com.canolabs.movieshelf.presentation.results

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.canolabs.movieshelf.R
import com.canolabs.movieshelf.domain.model.DataError
import com.canolabs.movieshelf.domain.model.Movie
import com.canolabs.movieshelf.presentation.common.messageRes
import com.canolabs.movieshelf.presentation.common.titleRes
import com.canolabs.movieshelf.presentation.components.POSTER_ASPECT_RATIO
import com.canolabs.movieshelf.presentation.components.PosterShape
import com.canolabs.movieshelf.presentation.components.SearchBarPill
import com.canolabs.movieshelf.presentation.components.StateMessage
import com.canolabs.movieshelf.presentation.results.ResultsContract.Content
import com.canolabs.movieshelf.presentation.results.ResultsContract.UiAction
import com.canolabs.movieshelf.presentation.results.ResultsContract.UiIntent
import com.canolabs.movieshelf.presentation.results.ResultsContract.UiState
import com.canolabs.movieshelf.presentation.results.components.MovieCard
import com.canolabs.movieshelf.presentation.results.ResultsContract.LoadMoreFailure
import com.canolabs.movieshelf.presentation.results.ResultsContract.NextPage
import com.canolabs.movieshelf.presentation.results.ResultsContract.Notice
import com.canolabs.movieshelf.presentation.results.components.ResultsNoticeBanner
import com.canolabs.movieshelf.ui.theme.AppTheme

@Composable
fun ResultsRoute(
    viewModel: ResultsViewModel,
    onBack: () -> Unit,
    onOpenMovie: (movieId: Long, posterUrl: String?) -> Unit,
    onOpenSearchEntry: (String) -> Unit,
) {
    // Observing the state is all the screen does; the ViewModel starts the submission itself.
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val stillOfflineMessage = stringResource(R.string.results_still_offline)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiActions.collect { action ->
                when (action) {
                    is UiAction.OpenMovieDetail -> onOpenMovie(action.movieId, action.posterUrl)
                    is UiAction.OpenSearchEntry -> onOpenSearchEntry(action.query)
                    // Transient confirmation only; the persistent offline notice stays in the UI state.
                    UiAction.ShowStillOffline -> launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(stillOfflineMessage)
                    }
                }
            }
        }
    }

    ResultsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun ResultsScreen(
    state: UiState,
    onIntent: (UiIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val title = if (state.isDiscover) stringResource(R.string.discover_results_title) else state.query
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Box {
            Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
                Box(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .widthIn(max = 720.dp)
                        .align(Alignment.CenterHorizontally),
                ) {
                    SearchBarPill(
                        text = title,
                        isPlaceholder = false,
                        onClick = { onIntent(UiIntent.SearchBarSelected) },
                        clickLabel = stringResource(R.string.results_edit_search, title),
                        leading = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                )
                            }
                        },
                    )
                }
                // The notice sits outside the scrollable content and above every content branch, so it is
                // visible for cached, cached-empty and local-match content alike, at any scroll position.
                ResultsNotice(
                    notice = state.notice,
                    showsLocalMatches = state.content is Content.LocalMatches,
                    lastUpdatedAt = state.lastUpdatedAt,
                    isRetrying = state.isRefreshing,
                    onRetry = { onIntent(UiIntent.Retry) },
                )
                PullToRefreshBox(
                    isRefreshing = state.showPullRefreshIndicator,
                    onRefresh = { onIntent(UiIntent.Retry) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AnimatedContent(
                        targetState = state.content,
                        contentKey = { it::class },
                        transitionSpec = {
                            (fadeIn(tween(220, delayMillis = 60)) + scaleIn(spring(Spring.DampingRatioLowBouncy), 0.96f))
                                .togetherWith(fadeOut(tween(120)))
                        },
                        label = "results-content",
                    ) { content ->
                        when (content) {
                            Content.Loading -> LoadingGrid(
                                if (state.isDiscover) {
                                    stringResource(R.string.discover_loading)
                                } else {
                                    stringResource(R.string.results_loading, state.query)
                                },
                            )
                            is Content.Movies -> MoviesGrid(
                                movies = content.movies,
                                header = pluralStringResource(R.plurals.results_count, content.movies.size, content.movies.size),
                                onIntent = onIntent,
                                nextPage = state.nextPage,
                            )
                            is Content.LocalMatches -> MoviesGrid(
                                movies = content.movies,
                                header = pluralStringResource(
                                    R.plurals.results_local_count,
                                    content.movies.size,
                                    content.movies.size,
                                ),
                                onIntent = onIntent,
                            )
                            is Content.Unavailable -> CenteredMessage {
                                StateMessage(
                                    icon = content.reason.unavailableIcon(),
                                    title = stringResource(content.reason.unavailableTitleRes()),
                                    message = content.reason.unavailableMessage(state.query, state.isDiscover),
                                    action = {
                                        RetryButton(isRetrying = state.isRefreshing, onRetry = { onIntent(UiIntent.Retry) })
                                    },
                                )
                            }
                            Content.Empty -> CenteredMessage {
                                StateMessage(
                                    icon = Icons.Rounded.SearchOff,
                                    title = stringResource(R.string.results_empty_title),
                                    message = if (state.isDiscover) {
                                        stringResource(R.string.discover_empty_message)
                                    } else {
                                        stringResource(R.string.results_empty_message, state.query)
                                    },
                                    action = {
                                        OutlinedButton(onClick = { onIntent(UiIntent.SearchBarSelected) }) {
                                            Text(stringResource(R.string.results_new_search))
                                        }
                                    },
                                )
                            }
                            is Content.Error -> CenteredMessage {
                                StateMessage(
                                    icon = content.error.icon(),
                                    title = stringResource(content.error.titleRes()),
                                    message = stringResource(content.error.messageRes()),
                                    action = {
                                        RetryButton(isRetrying = state.isRefreshing, onRetry = { onIntent(UiIntent.Retry) })
                                    },
                                )
                            }
                            Content.NotLoaded -> CenteredMessage {
                                StateMessage(
                                    icon = Icons.Rounded.Refresh,
                                    title = stringResource(R.string.results_interrupted_title),
                                    message = stringResource(R.string.results_interrupted_message, title),
                                    action = {
                                        RetryButton(isRetrying = state.isRefreshing, onRetry = { onIntent(UiIntent.Retry) })
                                    },
                                )
                            }
                        }
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
            )
        }
    }
}

private val gridColumns = GridCells.Adaptive(minSize = 156.dp)

@Composable
private fun ResultsNotice(
    notice: Notice?,
    showsLocalMatches: Boolean,
    lastUpdatedAt: Long?,
    isRetrying: Boolean,
    onRetry: () -> Unit,
) {
    // Remember the last notice so the exit animation still has content to draw.
    var shownNotice by remember { mutableStateOf(notice) }
    if (notice != null) shownNotice = notice
    AnimatedVisibility(
        visible = notice != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        shownNotice?.let {
            ResultsNoticeBanner(
                notice = it,
                showsLocalMatches = showsLocalMatches,
                lastUpdatedAt = lastUpdatedAt,
                isRetrying = isRetrying,
                onRetry = onRetry,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

/** Retry for the full-screen error states; shows progress and is disabled while a request runs. */
@Composable
private fun RetryButton(isRetrying: Boolean, onRetry: () -> Unit) {
    FilledTonalButton(onClick = onRetry, enabled = !isRetrying) {
        if (isRetrying) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.results_retrying))
        } else {
            Text(stringResource(R.string.action_retry))
        }
    }
}

@Composable
private fun MoviesGrid(
    movies: List<Movie>,
    header: String,
    onIntent: (UiIntent) -> Unit,
    nextPage: NextPage = NextPage.Unavailable,
) {
    LazyVerticalGrid(
        columns = gridColumns,
        contentPadding = gridPadding(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "count", span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = header,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        items(movies, key = { it.id }) { movie ->
            MovieCard(
                movie = movie,
                onClick = { onIntent(UiIntent.MovieSelected(movie.id)) },
                modifier = Modifier.animateItem(),
            )
        }
        if (nextPage != NextPage.Unavailable) {
            item(key = "load-more", span = { GridItemSpan(maxLineSpan) }) {
                LoadMoreFooter(nextPage = nextPage, onLoadMore = { onIntent(UiIntent.LoadMore) })
            }
        }
    }
}

/** End-of-list control for the next TMDB page; its loading and errors never replace loaded results. */
@Composable
private fun LoadMoreFooter(nextPage: NextPage, onLoadMore: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    ) {
        when (nextPage) {
            NextPage.Loading -> {
                CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.load_more_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is NextPage.Failed -> {
                Text(
                    text = stringResource(nextPage.reason.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = onLoadMore) { Text(stringResource(R.string.action_retry)) }
            }
            NextPage.Available -> OutlinedButton(onClick = onLoadMore) { Text(stringResource(R.string.load_more)) }
            NextPage.Unavailable -> Unit
        }
    }
}

private fun LoadMoreFailure.messageRes(): Int = when (this) {
    LoadMoreFailure.Offline -> R.string.load_more_offline
    is LoadMoreFailure.Error -> when (error) {
        DataError.Network -> R.string.load_more_unreachable
        DataError.Timeout -> R.string.load_more_timeout
        else -> R.string.load_more_failed
    }
}

/** Poster-shaped skeleton tiles: the layout of the content that is about to appear. */
@Composable
private fun LoadingGrid(loadingText: String) {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeleton-alpha",
    )
    Column(Modifier.fillMaxSize()) {
        Text(
            text = loadingText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        LazyVerticalGrid(
            columns = gridColumns,
            contentPadding = gridPadding(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics { },
        ) {
            items(12) {
                Column(Modifier.padding(4.dp).alpha(pulse)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(POSTER_ASPECT_RATIO)
                            .clip(PosterShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                    Spacer(Modifier.height(10.dp))
                    SkeletonLine(widthFraction = 0.8f)
                    Spacer(Modifier.height(6.dp))
                    SkeletonLine(widthFraction = 0.45f)
                }
            }
        }
    }
}

@Composable
private fun SkeletonLine(widthFraction: Float) {
    Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    // Scrollable container so pull-to-refresh also works on empty and error states.
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        contentPadding = PaddingValues(top = 48.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) { content() } }
    }
}

@Composable
private fun gridPadding(): PaddingValues {
    val bottom = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    return PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp + bottom)
}

private fun Notice.unavailableIcon() = when (this) {
    Notice.Offline -> Icons.Rounded.CloudOff
    Notice.BackOnline -> Icons.Rounded.CloudDone
    is Notice.RefreshFailed -> error.icon()
}

private fun Notice.unavailableTitleRes(): Int = when (this) {
    Notice.Offline -> R.string.results_offline_title
    Notice.BackOnline -> R.string.results_back_online_title
    is Notice.RefreshFailed -> error.titleRes()
}

@Composable
private fun Notice.unavailableMessage(query: String, isDiscover: Boolean): String = when (this) {
    Notice.Offline -> if (isDiscover) {
        stringResource(R.string.discover_offline_message)
    } else {
        stringResource(R.string.results_offline_no_matches, query)
    }
    Notice.BackOnline -> if (isDiscover) {
        stringResource(R.string.discover_back_online_message)
    } else {
        stringResource(R.string.results_back_online_message, query)
    }
    is Notice.RefreshFailed -> stringResource(error.messageRes())
}

private fun DataError.icon() = when (this) {
    DataError.Network -> Icons.Rounded.CloudOff
    DataError.Timeout -> Icons.Rounded.HourglassEmpty
    DataError.Unauthorized -> Icons.Rounded.Key
    else -> Icons.Rounded.ErrorOutline
}

@Preview(showBackground = true)
@Composable
private fun ResultsErrorPreview() {
    AppTheme(dynamicColor = false) {
        ResultsScreen(
            state = UiState(
                query = "Blade Runner",
                content = Content.Error(DataError.Network),
                isRefreshing = false,
                notice = null,
                lastUpdatedAt = null,
            ),
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultsContentPreview() {
    AppTheme(dynamicColor = false) {
        ResultsScreen(
            state = UiState(
                query = "Blade Runner",
                content = Content.Movies(
                    listOf(
                        Movie(78, "Blade Runner", null, "1982-06-25", null, 7.9, 14000),
                        Movie(335984, "Blade Runner 2049", null, "2017-10-04", null, 7.6, 13000),
                    ),
                ),
                isRefreshing = false,
                notice = Notice.Offline,
                lastUpdatedAt = System.currentTimeMillis() - 3_600_000,
            ),
            onIntent = {},
            onBack = {},
        )
    }
}
