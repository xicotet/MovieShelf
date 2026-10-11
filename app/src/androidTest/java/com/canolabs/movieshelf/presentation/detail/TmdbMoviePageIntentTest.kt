package com.canolabs.movieshelf.presentation.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the browser intent and the failure handling without actually leaving the app. */
@RunWith(AndroidJUnit4::class)
class TmdbMoviePageIntentTest {

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun buildsABrowsableViewIntentForTheMoviePage() {
        val intent = tmdbMovieIntent(603)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://www.themoviedb.org/movie/603", intent.dataString)
        assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
    }

    @Test
    fun startsTheViewIntent() {
        var started: Intent? = null
        val context = object : ContextWrapper(appContext) {
            override fun startActivity(intent: Intent) {
                started = intent
            }
        }

        assertTrue(context.openTmdbMoviePage(603))
        assertEquals("https://www.themoviedb.org/movie/603", started?.dataString)
    }

    @Test
    fun reportsFailureWhenNoAppCanOpenTheLink() {
        val context = object : ContextWrapper(appContext) {
            override fun startActivity(intent: Intent) = throw ActivityNotFoundException()
        }

        assertFalse(context.openTmdbMoviePage(603))
    }
}
