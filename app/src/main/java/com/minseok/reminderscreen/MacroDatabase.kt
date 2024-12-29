// MacroDatabase.kt
package com.minseok.reminderscreen.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.minseok.reminderscreen.data.dao.MacroDao
import com.minseok.reminderscreen.data.model.MacroTemplate
import com.minseok.reminderscreen.data.model.MacroItem

@Database(
    entities = [MacroTemplate::class, MacroItem::class],
    version = 1
)
abstract class MacroDatabase : RoomDatabase() {
    abstract fun macroDao(): MacroDao

    companion object {
        @Volatile
        private var instance: MacroDatabase? = null

        fun getInstance(context: Context): MacroDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): MacroDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MacroDatabase::class.java,
                "macro_database"
            ).build()
        }
    }
}