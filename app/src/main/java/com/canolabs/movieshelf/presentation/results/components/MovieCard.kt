package com.canolabs.movieshelf.presentation.results.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.canolabs.movieshelf.R
import com.canolabs.movieshelf.domain.model.Movie
import com.canolabs.movieshelf.presentation.common.formatRating
import com.canolabs.movieshelf.presentation.common.formatReleaseDate
import com.canolabs.movieshelf.presentation.components.POSTER_ASPECT_RATIO
import com.canolabs.movieshelf.presentation.components.PosterImage
import com.canolabs.movieshelf.presentation.components.sharedPoster

/** Poster-first result tile showing title, release date and (when rated) the TMDB score. */
@Composable
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val releaseDate = formatReleaseDate(movie.releaseDate)
    val rating = formatRating(movie.voteAverage, movie.voteCount)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(Modifier.padding(4.dp)) {
            PosterImage(
                url = movie.posterUrl,
                contentDescription = stringResource(R.string.poster_description, movie.title),
                modifier = Modifier
                    .sharedPoster(movie.id)
                    .fillMaxWidth()
                    .aspectRatio(POSTER_ASPECT_RATIO),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp).padding(bottom = 8.dp),
            ) {
                Text(
                    text = releaseDate ?: stringResource(R.string.detail_release_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (rating != null) {
                    val ratingDescription = stringResource(R.string.rating_description, rating)
                    Spacer(Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clearAndSetSemantics { contentDescription = ratingDescription },
                    ) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = rating,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

