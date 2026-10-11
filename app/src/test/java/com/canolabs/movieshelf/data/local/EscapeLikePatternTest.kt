package com.canolabs.movieshelf.data.local

import com.canolabs.movieshelf.data.local.datasource.escapeLikePattern
import org.junit.Assert.assertEquals
import org.junit.Test

class EscapeLikePatternTest {

    @Test
    fun `leaves ordinary text untouched`() {
        assertEquals("Blade Runner", escapeLikePattern("Blade Runner"))
    }

    @Test
    fun `escapes like wildcards and the escape character itself`() {
        assertEquals("100\\% Wolf", escapeLikePattern("100% Wolf"))
        assertEquals("a\\_b", escapeLikePattern("a_b"))
        assertEquals("back\\\\slash", escapeLikePattern("back\\slash"))
    }
}
