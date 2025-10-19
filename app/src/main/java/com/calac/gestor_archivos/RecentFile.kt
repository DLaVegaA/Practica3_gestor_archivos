package com.calac.gestor_archivos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files_table")
data class RecentFile(
    // La ruta sigue siendo la clave única
    @PrimaryKey val path: String,

    // Guardamos el nombre para mostrarlo
    val name: String,

    // ¡Importante! Guardamos la hora (en milisegundos) en que se abrió
    // Esto nos permite ordenar por "más reciente".
    val lastOpenedTimestamp: Long
)