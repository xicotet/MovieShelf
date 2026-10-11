package com.canolabs.movieshelf.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

val SearchBarHeight = 56.dp

/**
 * Thin outline that separates the search bar from light backgrounds (e.g. the Home gradient). Shared
 * by the bar and the full-screen search field, so the shared-bounds morph keeps the same edge.
 */
@Composable
fun searchBarBorder(): BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

/**
 * Collapsed, tappable search bar. It shares its bounds with the full-screen search field, so tapping
 * it morphs the bar into the Search Entry destination.
 */
@Composable
fun SearchBarPill(
    text: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    clickLabel: String? = null,
    /** Optional action at the end of the bar (e.g. settings); it keeps its own click and semantics. */
    trailing: (@Composable () -> Unit)? = null,
    leading: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = searchBarBorder(),
        modifier = modifier
            .sharedSearchBar()
            .fillMaxWidth()
            .height(SearchBarHeight)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (clickLabel != null) contentDescription = clickLabel
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, end = if (trailing != null) 4.dp else 20.dp),
        ) {
            leading()
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isPlaceholder) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
        }
    }
}
