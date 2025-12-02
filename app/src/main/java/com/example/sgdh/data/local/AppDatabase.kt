package com.example.sgdh.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BitacoraEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bitacoraDao(): BitacoraDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sgdh_local_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}