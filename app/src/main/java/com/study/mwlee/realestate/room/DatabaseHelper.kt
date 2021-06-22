package com.study.mwlee.realestate.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AptEntity::class, LocationEntity::class], version = 1)
abstract class DatabaseHelper : RoomDatabase() {

    abstract fun getAptDao(): AptDao
    abstract fun getLocationDao(): LocationDao

    companion object {
        private var INSTANCE: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper? {
            if (INSTANCE == null) {
                synchronized(DatabaseHelper::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        DatabaseHelper::class.java,
                        "real_estate.db"
                    ).build()
                }
            }
            return INSTANCE
        }
    }
}