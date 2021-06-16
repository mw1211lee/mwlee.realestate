package com.study.mwlee.realestate.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AptEntity::class], version = 1)
abstract class AptDatabase : RoomDatabase() {

    abstract fun getAptDao(): AptDao

    companion object {
        private var INSTANCE: AptDatabase? = null

        fun getInstance(context: Context): AptDatabase? {
            if (INSTANCE == null) {
                synchronized(AptDatabase::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        AptDatabase::class.java,
                        "apt.db"
                    ).build()
                }
            }
            return INSTANCE
        }
    }
}