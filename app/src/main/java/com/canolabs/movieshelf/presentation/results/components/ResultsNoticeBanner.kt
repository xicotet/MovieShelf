package com.canolabs.movieshelf.presentation.results.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.canolabs.movieshelf.R
import com.canolabs.movieshelf.domain.model.DataError
import com.canolabs.movieshelf.presentation.common.formatRelativeTime
import com.canolabs.movieshelf.presentation.results.ResultsContract.Notice

/**
 * Persistent notice above content that may be incomplete or out of date: saved TMDB results shown
 * offline or after a failed refresh, or saved title matches shown instead of a TMDB search. While a
 * retry runs it stays visible, reports progress and disables Retry.
 */
@Composable
fun ResultsNoticeBanner(
    notice: Notice,
    showsLocalMatches: Boolean,
    lastUpdatedAt: Long?,
    isRetrying: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Icon(notice.icon(), contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = stringResource(notice.messageRes(showsLocalMatches)), style = MaterialTheme.typography.bodyMedium)
                when {
                    isRetrying -> Text(
                        text = stringResource(R.string.results_retrying),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    !showsLocalMatches && lastUpdatedAt != null -> Text(
                        text = stringResource(
                            R.string.results_updated,
                            formatRelativeTime(lastUpdatedAt, System.currentTimeMillis()),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            TextButton(onClick = onRetry, enabled = !isRetrying) {
                if (isRetrying) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(stringResource(R.string.action_retry))
                }
            }
        }
    }
}

private fun Notice.icon(): ImageVector = when (this) {
    Notice.Offline -> Icons.Rounded.CloudOff
    Notice.BackOnline -> Icons.Rounded.CloudDone
    is Notice.RefreshFailed -> when (error) {
        DataError.Network -> Icons.Rounded.CloudOff
        DataError.Timeout -> Icons.Rounded.HourglassEmpty
        else -> Icons.Rounded.ErrorOutline
    }
}

private fun Notice.messageRes(localMatches: Boolean): Int = when (this) {
    Notice.Offline -> if (localMatches) R.string.results_notice_offline_local else R.string.results_notice_offline_saved
    Notice.BackOnline -> if (localMatches) R.string.results_notice_back_online_local else R.string.results_notice_back_online_saved
    is Notice.RefreshFailed -> when (error) {
        DataError.Network -> if (localMatches) R.string.results_notice_network_local else R.string.results_stale_network
        DataError.Timeout -> if (localMatches) R.string.results_notice_timeout_local else R.string.results_stale_timeout
        else -> R.string.results_stale_generic
    }
}
