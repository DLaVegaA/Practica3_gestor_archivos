package com.calac.gestor_archivos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File // Importa File para usarlo en getFileIconResId
import java.util.Locale

class RecentFilesAdapter(
    private var recentFiles: List<RecentFile>,
    private val onItemClickListener: (RecentFile, View) -> Unit
) : RecyclerView.Adapter<RecentFilesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconImageView: ImageView = view.findViewById(R.id.iconImageView)
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val favoriteStar: ImageView = view.findViewById(R.id.iv_favorite_star) // Lo ocultaremos
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.file_item, parent, false) // Reutilizamos el layout
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recentFile = recentFiles[position]
        holder.nameTextView.text = recentFile.name
        holder.iconImageView.setImageResource(getFileIconResId(recentFile.name)) // Usamos el nombre para el ícono
        holder.favoriteStar.visibility = View.GONE // Ocultamos la estrella de favoritos

        holder.itemView.setOnClickListener {
            onItemClickListener(recentFile, holder.itemView)
        }
    }

    override fun getItemCount(): Int = recentFiles.size

    fun updateData(newRecentFiles: List<RecentFile>) {
        recentFiles = newRecentFiles
        notifyDataSetChanged()
    }

    // Función de ayuda para obtener el ícono (copiada de FileAdapter)
    private fun getFileIconResId(fileName: String): Int {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.getDefault())
        return when (extension) {
            // Copia aquí el bloque 'when' completo de tu FileAdapter
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> R.drawable.ic_file_image
            // ... (resto de extensiones) ...
            else -> R.drawable.ic_file_generic
        }
    }
}