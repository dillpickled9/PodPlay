package com.raywenderlich.podplay.db

import PodcastDao
import android.content.Context
import androidx.room.*
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import kotlinx.coroutines.CoroutineScope
import java.util.*


@Database(entities = [Podcast::class, Episode::class], version = 1)
@TypeConverters(PodPlayDatabase.Companion.Converters::class)
abstract class PodPlayDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao


    companion object {

        @Volatile
        private var INSTANCE: PodPlayDatabase? = null


        class Converters {
            @TypeConverter
            fun fromTimestamp(value: Long?): Date? {
                return if (value == null) null else Date(value)
            }
            @TypeConverter
            fun toTimestamp(date: Date?): Long? {
                return (date?.time)
            }
        }

        fun getInstance(context: Context, coroutineScope:
        CoroutineScope
        ): PodPlayDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            synchronized(this) {
                val instance =
                    Room.databaseBuilder(context.applicationContext,
                        PodPlayDatabase::class.java,
                        "PodPlayer")
                        .build()
                INSTANCE = instance

                return instance
            }
        }
    }
}