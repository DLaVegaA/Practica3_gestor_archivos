package com.calac.gestor_archivos

import androidx.room.Entity
import androidx.room.PrimaryKey

// @Entity le dice a Room que esta clase es una tabla en la base de datos
@Entity(tableName = "favorites_table")
data class Favorite(
    // @PrimaryKey le dice a Room que 'path' es la clave única.
    // No se pueden guardar dos favoritos con la misma ruta.
    @PrimaryKey val path: String,

    // Guardamos estos datos extra para no tener que leer el archivo
    // cada vez que mostremos la lista de favoritos.
    val name: String,
    val isDirectory: Boolean
)