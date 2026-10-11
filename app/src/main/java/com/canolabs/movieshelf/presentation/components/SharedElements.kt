package com.canolabs.movieshelf.presentation.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope

/** Provided by the app's navigation host; `null` in previews. */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val PosterShape: Shape = RoundedCornerShape(12.dp)

object SharedElementKeys {
    fun poster(movieId: Long): String = "poster-$movieId"
    const val SEARCH_BAR = "search-bar"
}

/** Poster that travels between Results and Detail. Keyed by movie ID so the pairing is stable. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedPoster(movieId: Long): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedContentScope.current
    return with(sharedScope) {
        this@sharedPoster.sharedElement(
            sharedContentState = rememberSharedContentState(SharedElementKeys.poster(movieId)),
            animatedVisibilityScope = animatedScope,
            clipInOverlayDuringTransition = OverlayClip(PosterShape),
        )
    }
}

/** Container of the search field, morphing between Search/Results and the full-screen Search Entry. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedSearchBar(): Modifier {
    val sharedScope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedContentScope.current
    return with(sharedScope) {
        this@sharedSearchBar.sharedBounds(
            sharedContentState = rememberSharedContentState(SharedElementKeys.SEARCH_BAR),
            animatedVisibilityScope = animatedScope,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}
