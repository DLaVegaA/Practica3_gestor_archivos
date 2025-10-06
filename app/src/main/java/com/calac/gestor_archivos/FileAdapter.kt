package com.calac.gestor_archivos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileAdapter(private var files: List<FileItem>,
                  private val onItemClickListener: (FileItem) -> Unit
) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

    // Esta clase interna representa una vista de fila en el RecyclerView.
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconImageView: ImageView = view.findViewById(R.id.iconImageView)
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
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
        val fileItem = files[position]

        // Pone el nombre del archivo en el TextView.
        holder.nameTextView.text = fileItem.name

        // Elige un icono diferente si es una carpeta o un archivo.
        if (fileItem.isDirectory) {
            holder.iconImageView.setImageResource(android.R.drawable.ic_menu_manage) // Icono genérico de carpeta
        } else {
            holder.iconImageView.setImageResource(android.R.drawable.ic_menu_agenda) // Icono genérico de archivo
        }

        holder.itemView.setOnClickListener {
            onItemClickListener(fileItem)
        }
    }

    // Devuelve el número total de elementos en la lista.
    override fun getItemCount(): Int {
        return files.size
    }

    // Un método útil para actualizar la lista de archivos del adapter desde la Activity.
    fun updateData(newFiles: List<FileItem>) {
        files = newFiles
        notifyDataSetChanged() // Notifica al RecyclerView que los datos han cambiado y debe redibujarse.
    }
}