package com.canolabs.movieshelf.app.navigation

import androidx.navigation3.runtime.NavKey
import com.canolabs.movieshelf.domain.model.ResultsRequest
import com.canolabs.movieshelf.testing.query
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ShowResultsTest {

    private var nextId = 0
    private val ids = { "submission-${++nextId}" }

    @Test
    fun `submitting from search replaces the entry with results`() {
        val backStack = mutableListOf<NavKey>(HomeDestination, SearchEntryDestination())

        backStack.showResults(query("alien"), ids)

        assertEquals(listOf(HomeDestination, ResultsDestination("alien", "submission-1")), backStack)
    }

    @Test
    fun `resubmitting the same query creates a distinct entry that replaces the previous one`() {
        val backStack = mutableListOf<NavKey>(HomeDestination, SearchEntryDestination())
        backStack.showResults(query("alien"), ids)
        val first = backStack.last()

        // Results search bar -> Search Entry prefilled with the same text -> submit again.
        backStack.add(SearchEntryDestination("alien"))
        backStack.showResults(query("alien"), ids)
        val second = backStack.last()

        assertEquals(listOf(HomeDestination, second), backStack)
        assertNotEquals(first, second)
        // Navigation 3's default content key (and so the ViewModel scope) is key.toString().
        assertNotEquals(first.toString(), second.toString())
    }

    @Test
    fun `default ids are unique per submission`() {
        val backStack = mutableListOf<NavKey>(HomeDestination)

        backStack.showResults(query("alien"))
        val first = backStack.last() as ResultsDestination
        backStack.showResults(query("alien"))
        val second = backStack.last() as ResultsDestination

        assertNotEquals(first.submissionId, second.submissionId)
    }

    @Test
    fun `discover opens results without a query`() {
        val backStack = mutableListOf<NavKey>(HomeDestination)

        backStack.showDiscover(ids)

        assertEquals(listOf(HomeDestination, ResultsDestination(query = null, submissionId = "submission-1")), backStack)
    }

    @Test
    fun `a results entry maps to the request it stands for`() {
        assertEquals(ResultsRequest.Discover, ResultsDestination(query = null, submissionId = "1").toRequest())
        assertEquals(ResultsRequest.Search(query("Alien")), ResultsDestination(query = "Alien", submissionId = "1").toRequest())
    }
}
