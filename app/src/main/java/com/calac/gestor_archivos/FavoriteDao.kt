package com.calac.gestor_archivos

import androidx.room.*

@Dao
interface FavoriteDao {

    // (onConflict = OnConflictStrategy.REPLACE) significa que si
    // intentamos insertar un favorito que ya existe, simplemente se reemplaza.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: Favorite) // 'suspend' es para coroutines (hacerlo fuera del hilo principal)

    @Delete
    suspend fun delete(favorite: Favorite)

    // Consulta para obtener todos los favoritos
    @Query("SELECT * FROM favorites_table ORDER BY name ASC")
    suspend fun getAllFavorites(): List<Favorite>

    // Consulta para ver si UN archivo específico es favorito
    @Query("SELECT * FROM favorites_table WHERE path = :path LIMIT 1")
    suspend fun getFavoriteByPath(path: String): Favorite? // Devuelve null si no se encuentra
}