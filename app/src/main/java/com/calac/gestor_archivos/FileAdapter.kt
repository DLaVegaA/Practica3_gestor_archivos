package com.calac.gestor_archivos

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class FileAdapter(private var files: List<FileItem>,
                  private val onItemClickListener: (FileItem, View) -> Unit,
                  private val onItemLongClickListener: (FileItem) -> Unit
) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

    private var unfilteredFileList: List<FileItem> = listOf()
    private var favoritePaths = setOf<String>()

    // Cuando el adapter se crea, guardamos la lista original
    init {
        this.unfilteredFileList = files
    }

    // Esta clase interna representa una vista de fila en el RecyclerView.
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconImageView: ImageView = view.findViewById(R.id.iconImageView)
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val favoriteStar: ImageView = view.findViewById(R.id.iv_favorite_star)
    }

    // Se llama cuando RecyclerView necesita una nueva vista de fila.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // "Infla" (crea) la vista de la fila usando nuestro layout file_item.xml
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.file_item, parent, false)
        return ViewHolder(view)
    }

    // Se llama para mostrar los datos en una posición específica.
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        val fileItem = files[position]
        // Pone el nombre del archivo en el TextView.
        holder.nameTextView.text = fileItem.name
        // Elige un icono diferente si es una carpeta o un archivo.
        // 1. Obtenemos el ID del ícono correcto
        val iconResId = getFileIconResId(fileItem)
        // 2. Lo asignamos al ImageView
        holder.iconImageView.setImageResource(iconResId)

        // Comprueba si la ruta de este archivo está en la lista de favoritos
        if (favoritePaths.contains(fileItem.path)) {
            // Si es favorito, muestra la estrella rellena
            holder.favoriteStar.setImageResource(R.drawable.ic_star_filled)
            holder.favoriteStar.visibility = View.VISIBLE
        } else {
            // Si no es favorito, oculta la estrella
            holder.favoriteStar.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClickListener(fileItem, holder.itemView) // <-- MODIFICADO
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClickListener(fileItem)
            true // Devuelve 'true' para indicar que el evento ha sido consumido
        }
    }

    // Devuelve el número total de elementos en la lista.
    override fun getItemCount(): Int {
        return files.size
    }

    // Un método útil para actualizar la lista de archivos del adapter desde la Activity.
    fun updateData(newFiles: List<FileItem>) {
        files = newFiles
        unfilteredFileList = newFiles
        notifyDataSetChanged() // Notifica al RecyclerView que los datos han cambiado y debe redibujarse.
    }

    /**
     * Filtra la lista de archivos basada en un texto de búsqueda.
     */
    fun filter(query: String) {
        files = if (query.isEmpty()) {
            // Si la búsqueda está vacía, restaura la lista completa
            unfilteredFileList
        } else {
            // Si hay texto, filtra la lista completa
            unfilteredFileList.filter {
                // Comprueba si el nombre del archivo contiene el texto, ignorando mayúsculas
                it.name.contains(query, ignoreCase = true)
            }
        }
        // Notifica al RecyclerView que los datos han cambiado
        notifyDataSetChanged()
    }

    /**
     * Devuelve el ID del recurso (drawable) del ícono apropiado para el archivo.
     */
    private fun getFileIconResId(fileItem: FileItem): Int {
        // 1. Si es una carpeta, devuelve el ícono de carpeta
        if (fileItem.isDirectory) {
            return R.drawable.ic_folder
        }

        // 2. Si es un archivo, obtén la extensión
        val extension = fileItem.name.substringAfterLast('.', "").lowercase(Locale.getDefault())

        // 3. Devuelve un ícono basado en la extensión
        return when (extension) {
            // Imágenes
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> R.drawable.ic_file_image
            // Audio
            "mp3", "wav", "aac", "ogg", "m4a" -> R.drawable.ic_file_audio
            // Video
            "mp4", "mkv", "avi", "webm", "3gp" -> R.drawable.ic_file_video
            // Documentos
            "pdf" -> R.drawable.ic_file_pdf
            "txt", "md", "log" -> R.drawable.ic_file_text
            // Código
            "xml", "json", "kt", "java", "html", "css" -> R.drawable.ic_file_code
            // Comprimidos
            "zip", "rar", "7z", "tar" -> R.drawable.ic_folder_zip
            // Si no coincide con nada, usa el ícono genérico
            else -> R.drawable.ic_file_generic
        }
    }

    /**
     * Actualiza la lista de favoritos y refresca el RecyclerView.
     */
    fun updateFavorites(newFavoritePaths: Set<String>) {
        favoritePaths = newFavoritePaths
        notifyDataSetChanged() // Refresca la lista para mostrar/ocultar estrellas
    }
}