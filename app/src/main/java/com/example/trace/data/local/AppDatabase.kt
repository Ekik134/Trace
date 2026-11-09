package com.example.trace.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.trace.data.local.dao.FileDao
import com.example.trace.data.local.dao.TagDao
import com.example.trace.data.local.entity.FileEntity
import com.example.trace.data.local.entity.TagEntity
import com.example.trace.data.local.entity.FileTagCrossRef
import com.example.trace.data.local.entity.TagRelationEntity

// Аннотация @Database перечисляет все ваши сущности
@Database(
    entities = [
        FileEntity::class,
        TagEntity::class,
        FileTagCrossRef::class,
        TagRelationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun fileDao(): FileDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trace_database" // Имя файла БД на устройстве
                )
                    .fallbackToDestructiveMigration() // При изменении схемы БД пересоздастся (для разработки)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}