package com.canolabs.movieshelf.presentation.searchentry

import com.canolabs.movieshelf.domain.model.RecentSearch
import com.canolabs.movieshelf.domain.usecase.ObserveRecentSearchesUseCaseImpl
import com.canolabs.movieshelf.testing.FakeSearchRepository
import com.canolabs.movieshelf.testing.MainDispatcherRule
import com.canolabs.movieshelf.testing.query
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSearchRepository()
    private val viewModel by lazy { SearchEntryViewModel(ObserveRecentSearchesUseCaseImpl(repository)) }

    @Test
    fun `initial state has no recent searches loaded yet`() {
        val state = viewModel.uiState.value

        assertFalse(state.isLoaded)
        assertTrue(state.recentSearches.isEmpty())
    }

    @Test
    fun `exposes persisted recent searches`() = runTest {
        val recent = listOf(RecentSearch(query("Heat"), 2L), RecentSearch(query("Alien"), 1L))
        repository.setRecent(*recent.toTypedArray())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertTrue(state.isLoaded)
        assertEquals(recent, state.recentSearches)
    }

    @Test
    fun `validation rejects blank input and normalizes submitted text`() {
        assertNull(viewModel.validate("    "))
        assertEquals("Blade Runner", viewModel.validate("  Blade  Runner ")?.text)
    }

    @Test
    fun `typing and observing history never issues a search request`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        listOf("B", "Ba", "Bat", "Batman").forEach { viewModel.validate(it) }

        assertTrue(repository.refreshCalls.isEmpty())
    }
}
