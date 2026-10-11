package com.canolabs.movieshelf.presentation.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TmdbMoviePageTest {

    @Test
    fun `builds the canonical tmdb movie url from the movie id`() {
        assertEquals("https://www.themoviedb.org/movie/603", tmdbMovieUrl(603))
    }

    @Test
    fun `the url carries no credentials`() {
        assertFalse(tmdbMovieUrl(603).contains("api_key"))
    }
}
