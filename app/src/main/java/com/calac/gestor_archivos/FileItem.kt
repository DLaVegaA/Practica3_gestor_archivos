package com.calac.gestor_archivos

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeInBytes: Long,
    val modifiedDate: Long
)