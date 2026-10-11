package com.canolabs.movieshelf.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.canolabs.movieshelf.data.local.dao.MovieDao
import com.canolabs.movieshelf.data.local.dao.SearchDao
import com.canolabs.movieshelf.data.local.entity.MovieEntity
import com.canolabs.movieshelf.data.local.entity.SearchQueryEntity
import com.canolabs.movieshelf.data.local.entity.SearchResultCrossRef

@Database(
    entities = [MovieEntity::class, SearchQueryEntity::class, SearchResultCrossRef::class],
    version = 1,
    exportSchema = true,
)
abstract class MovieShelfDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun searchDao(): SearchDao

    companion object {
        const val NAME = "movieshelf.db"
    }
}
