package com.canolabs.movieshelf.presentation.searchentry

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.canolabs.movieshelf.domain.model.RecentSearch
import com.canolabs.movieshelf.domain.model.SearchQuery
import com.canolabs.movieshelf.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentSearchLongPressTest {

    @get:Rule
    val compose = createComposeRule()

    private val submitted = mutableListOf<SearchQuery>()
    private val removed = mutableListOf<SearchQuery>()
    private fun query(text: String) = requireNotNull(SearchQuery.from(text))

    @Before
    fun setUp() {
        compose.setContent {
            AppTheme(dynamicColor = false) {
                SearchEntryScreen(
                    initialQuery = "",
                    state = SearchEntryUiState(
                        recentSearches = listOf(RecentSearch(query("Alien"), 2), RecentSearch(query("Heat"), 1)),
                        isLoaded = true,
                    ),
                    validate = SearchQuery::from,
                    onBack = {},
                    onSubmit = { submitted += it },
                    onRemoveRecent = { removed += it },
                )
            }
        }
    }

    private fun longPressAlien() = compose.onNodeWithText("Alien").performTouchInput { longClick() }

    @Test
    fun longPressOpensTheDialogWithoutSearching() {
        longPressAlien()

        compose.onNodeWithText("Remove recent search?").assertIsDisplayed()
        assertTrue(submitted.isEmpty())
    }

    @Test
    fun cancelDismissesWithoutRemovingAnything() {
        longPressAlien()

        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithText("Remove recent search?").assertDoesNotExist()
        assertTrue(removed.isEmpty())
    }

    @Test
    fun deleteRemovesOnlyTheSelectedEntry() {
        longPressAlien()

        compose.onNodeWithText("Delete").performClick()

        compose.onNodeWithText("Remove recent search?").assertDoesNotExist()
        assertEquals(listOf(query("Alien")), removed)
        assertTrue(submitted.isEmpty())
    }

    @Test
    fun aNormalTapStillSearches() {
        compose.onNodeWithText("Heat").performClick()

        assertEquals(listOf(query("Heat")), submitted)
        assertTrue(removed.isEmpty())
    }
}
