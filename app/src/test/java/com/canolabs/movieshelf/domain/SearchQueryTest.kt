package com.canolabs.movieshelf.domain

import com.canolabs.movieshelf.domain.model.SearchQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchQueryTest {

    @Test
    fun `rejects blank input`() {
        assertNull(SearchQuery.from(""))
        assertNull(SearchQuery.from("   \t\n"))
    }

    @Test
    fun `trims and collapses whitespace while keeping the display casing`() {
        val query = SearchQuery.from("  The   Dark Knight ")!!

        assertEquals("The Dark Knight", query.text)
        assertEquals("the dark knight", query.key)
    }

    @Test
    fun `queries with the same normalized key are equal`() {
        assertEquals(SearchQuery.from("ALIEN"), SearchQuery.from(" alien "))
    }
}
