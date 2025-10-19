package com.calac.gestor_archivos

import androidx.room.*

@Dao
interface RecentFileDao {

    // Si abrimos un archivo que ya está en el historial,
    // REPLACE actualizará su 'lastOpenedTimestamp'.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(recentFile: RecentFile)

    // Obtiene los archivos recientes, ordenados del más nuevo al más viejo
    @Query("SELECT * FROM recent_files_table ORDER BY lastOpenedTimestamp DESC")
    suspend fun getAllRecents(): List<RecentFile>

    // (Opcional) Borra los más antiguos si la lista excede un límite (ej. 20)
    @Query("DELETE FROM recent_files_table WHERE path NOT IN (SELECT path FROM recent_files_table ORDER BY lastOpenedTimestamp DESC LIMIT :limit)")
    suspend fun trimHistory(limit: Int)

    // (Opcional) Borra un archivo específico del historial
    @Delete
    suspend fun delete(recentFile: RecentFile)
}