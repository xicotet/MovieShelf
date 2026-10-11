package com.canolabs.movieshelf.presentation.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Canonical public TMDB page of a movie: `https://www.themoviedb.org/movie/{id}`. No credentials. */
fun tmdbMovieUrl(movieId: Long): String = "https://www.themoviedb.org/movie/$movieId"

/** Opens [tmdbMovieUrl] in whatever app handles web links (normally the browser). */
fun tmdbMovieIntent(movieId: Long): Intent =
    Intent(Intent.ACTION_VIEW, tmdbMovieUrl(movieId).toUri()).addCategory(Intent.CATEGORY_BROWSABLE)

/** Returns `false` when no installed app can open the page, instead of crashing. */
fun Context.openTmdbMoviePage(movieId: Long): Boolean = try {
    startActivity(tmdbMovieIntent(movieId))
    true
} catch (_: ActivityNotFoundException) {
    false
}
