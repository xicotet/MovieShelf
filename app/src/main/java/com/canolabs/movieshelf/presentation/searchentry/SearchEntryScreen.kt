package com.canolabs.movieshelf.presentation.searchentry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canolabs.movieshelf.R
import com.canolabs.movieshelf.domain.model.RecentSearch
import com.canolabs.movieshelf.domain.model.SearchQuery
import com.canolabs.movieshelf.presentation.components.searchBarBorder
import com.canolabs.movieshelf.presentation.components.sharedSearchBar
import com.canolabs.movieshelf.ui.theme.AppTheme

@Composable
fun SearchEntryRoute(
    initialQuery: String,
    viewModel: SearchEntryViewModel,
    onBack: () -> Unit,
    onSubmit: (SearchQuery) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchEntryScreen(
        initialQuery = initialQuery,
        state = state,
        validate = viewModel::validate,
        onBack = onBack,
        onSubmit = onSubmit,
        onRemoveRecent = viewModel::removeRecent,
    )
}

@Composable
fun SearchEntryScreen(
    initialQuery: String,
    state: SearchEntryUiState,
    validate: (String) -> SearchQuery?,
    onBack: () -> Unit,
    onSubmit: (SearchQuery) -> Unit,
    onRemoveRecent: (SearchQuery) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Text of the recent search whose removal is being confirmed; survives configuration changes.
    var pendingRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    pendingRemoval?.let { text ->
        RemoveRecentSearchDialog(
            queryText = text,
            onConfirm = {
                SearchQuery.from(text)?.let(onRemoveRecent)
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null },
        )
    }
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(textFieldValueAtEnd(initialQuery))
    }
    val submittable = validate(field.text)
    val submit = { submittable?.let(onSubmit) }

    // Filtering local history while typing is purely local; no request is issued until submit.
    val typedKey = SearchQuery.from(field.text)?.key
    val visibleRecents = remember(state.recentSearches, typedKey) {
        if (typedKey == null) state.recentSearches
        else state.recentSearches.filter { it.query.key.contains(typedKey) && it.query.key != typedKey }
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SearchField(
                value = field,
                onValueChange = { field = it },
                canSubmit = submittable != null,
                onSubmit = { submit() },
                onBack = onBack,
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                if (submittable != null) {
                    item(key = "submit-hint") {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(R.string.search_entry_submit_hint, submittable.text),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingContent = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.clickable { submit() },
                        )
                    }
                }
                item(key = "recent-header") {
                    Text(
                        text = stringResource(R.string.search_entry_recent),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
                    )
                }
                if (state.isLoaded && visibleRecents.isEmpty()) {
                    item(key = "recent-empty") {
                        Text(
                            text = stringResource(
                                if (state.recentSearches.isEmpty()) R.string.search_entry_no_recent
                                else R.string.search_entry_no_matching_recent,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        )
                    }
                }
                items(visibleRecents, key = { it.query.key }) { recent ->
                    RecentSearchItem(
                        recent = recent,
                        onClick = { onSubmit(recent.query) },
                        onLongClick = { pendingRemoval = recent.query.text },
                        onFill = { field = textFieldValueAtEnd(recent.query.text) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    canSubmit: Boolean,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    // Focus the field as soon as the destination opens so the keyboard shows up, as in Maps.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = searchBarBorder(),
        modifier = modifier
            .sharedSearchBar()
            .fillMaxWidth(),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                val placeholder = stringResource(R.string.search_placeholder)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(onSearch = { if (canSubmit) onSubmit() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = placeholder },
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(vertical = 16.dp)) {
                            if (value.text.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
                AnimatedVisibility(visible = value.text.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = { onValueChange(TextFieldValue("")) }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear_text))
                    }
                }
                IconButton(onClick = onSubmit, enabled = canSubmit) {
                    Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.action_search))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
        }
    }
}

@Composable
private fun RecentSearchItem(
    recent: RecentSearch,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFill: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(recent.query.text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingContent = {
            IconButton(onClick = onFill) {
                Icon(
                    Icons.Rounded.NorthWest,
                    contentDescription = stringResource(R.string.action_fill_query, recent.query.text),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
            // Announced by TalkBack, which also exposes the long-press action.
            onLongClickLabel = stringResource(R.string.recent_remove_action),
        ),
    )
}

/** Confirms removing one recent search. Long-pressing never starts a search. */
@Composable
private fun RemoveRecentSearchDialog(queryText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recent_remove_title)) },
        text = { Text(stringResource(R.string.recent_remove_message, queryText)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private fun textFieldValueAtEnd(text: String) = TextFieldValue(text, selection = TextRange(text.length))

@Preview(showBackground = true)
@Composable
private fun SearchEntryPreview() {
    AppTheme(dynamicColor = false) {
        SearchEntryScreen(
            initialQuery = "",
            state = SearchEntryUiState(
                recentSearches = listOfNotNull(SearchQuery.from("Blade Runner"), SearchQuery.from("Arrival"))
                    .map { RecentSearch(it, 0L) },
                isLoaded = true,
            ),
            validate = SearchQuery::from,
            onBack = {},
            onSubmit = {},
            onRemoveRecent = {},
        )
    }
}
