package com.canolabs.movieshelf.app.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.canolabs.movieshelf.domain.model.ResultsRequest
import com.canolabs.movieshelf.domain.model.SearchQuery
import com.canolabs.movieshelf.presentation.components.LocalSharedTransitionScope
import com.canolabs.movieshelf.presentation.detail.MovieDetailRoute
import com.canolabs.movieshelf.presentation.home.HomeRoute
import com.canolabs.movieshelf.presentation.results.ResultsRoute
import com.canolabs.movieshelf.presentation.searchentry.SearchEntryRoute
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.UUID

/**
 * Home -> Search Entry -> Results -> Movie Detail, on a Navigation 3 back stack.
 *
 * Every destination is wrapped in one [SharedTransitionLayout] whose scope is handed to [NavDisplay],
 * so the search bar can morph into the full-screen entry field and posters can travel from Results to
 * Detail. Screen transitions are cross-fades so that the shared elements carry the motion.
 */
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(HomeDestination)

    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                sharedTransitionScope = this,
                transitionSpec = { fadeThrough() },
                popTransitionSpec = { fadeThrough() },
                predictivePopTransitionSpec = { fadeThrough() },
                entryProvider = entryProvider {
                    entry<HomeDestination> {
                        HomeRoute(
                            settingsViewModel = koinViewModel(),
                            onSearchClick = { backStack.add(SearchEntryDestination()) },
                            onDiscoverClick = { backStack.showDiscover() },
                        )
                    }
                    entry<SearchEntryDestination> { key ->
                        SearchEntryRoute(
                            initialQuery = key.initialQuery,
                            viewModel = koinViewModel(),
                            onBack = { backStack.removeLastOrNull() },
                            onSubmit = { query -> backStack.showResults(query) },
                        )
                    }
                    entry<ResultsDestination> { key ->
                        ResultsRoute(
                            viewModel = koinViewModel { parametersOf(key.toRequest()) },
                            onBack = { backStack.removeLastOrNull() },
                            onOpenMovie = { movieId, posterUrl -> backStack.add(MovieDetailDestination(movieId, posterUrl)) },
                            onOpenSearchEntry = { query -> backStack.add(SearchEntryDestination(query)) },
                        )
                    }
                    entry<MovieDetailDestination> { key ->
                        MovieDetailRoute(
                            viewModel = koinViewModel { parametersOf(key.movieId) },
                            posterUrlHint = key.posterUrl,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                },
            )
        }
    }
}

/**
 * Replaces the search entry (and the results it was opened from, if any) with the results for
 * [query], so Back from Results always returns to Home rather than to an older query. Every call
 * creates a new submission identity, including for identical query text.
 */
internal fun MutableList<NavKey>.showResults(
    query: SearchQuery,
    newSubmissionId: () -> String = { UUID.randomUUID().toString() },
) {
    if (lastOrNull() is SearchEntryDestination) removeAt(lastIndex)
    if (lastOrNull() is ResultsDestination) removeAt(lastIndex)
    add(ResultsDestination(query.text, newSubmissionId()))
}

/** Opens Discover as a new Results submission on top of Home. */
internal fun MutableList<NavKey>.showDiscover(
    newSubmissionId: () -> String = { UUID.randomUUID().toString() },
) {
    add(ResultsDestination(query = null, submissionId = newSubmissionId()))
}

/** The request a Results entry stands for: its typed query, or Discover when it has none. */
internal fun ResultsDestination.toRequest(): ResultsRequest =
    query?.let { ResultsRequest.Search(requireNotNull(SearchQuery.from(it)) { "Results opened for a blank query" }) }
        ?: ResultsRequest.Discover

/** Material fade-through with a soft spring scale; shared elements carry the spatial motion. */
private fun fadeThrough(): ContentTransform {
    val enter = fadeIn(tween(durationMillis = 220, delayMillis = 90)) +
        scaleIn(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow), initialScale = 0.98f)
    val exit = fadeOut(tween(durationMillis = 90))
    return enter togetherWith exit
}
