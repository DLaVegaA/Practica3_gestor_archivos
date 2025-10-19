package com.calac.gestor_archivos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// --- ▼▼▼ MODIFICACIÓN 1: Añade RecentFile a la lista de entidades ▼▼▼ ---
@Database(entities = [Favorite::class, RecentFile::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Conecta con el DAO de Favoritos
    abstract fun favoriteDao(): FavoriteDao

    // --- ▼▼▼ MODIFICACIÓN 2: Añade el DAO de Recientes ▼▼▼ ---
    abstract fun recentFileDao(): RecentFileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "file_manager_database"
                ).build() // build() puede necesitar migraciones si cambias 'version'
                INSTANCE = instance
                instance
            }
        }
    }
}