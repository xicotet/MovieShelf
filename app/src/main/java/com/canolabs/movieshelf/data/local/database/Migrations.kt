package com.canolabs.movieshelf.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: pagination metadata per cached query.
 *
 * Queries that already had a successful refresh hold exactly one page; their `total_pages` is
 * unknown (v1 never stored it), so "Load more" stays hidden for them until the next refresh stores
 * TMDB's real value. Nothing else changes, and no data is dropped.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE search_queries ADD COLUMN loaded_pages INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE search_queries ADD COLUMN total_pages INTEGER")
        db.execSQL("UPDATE search_queries SET loaded_pages = 1 WHERE last_refreshed_at IS NOT NULL")
    }
}
