package com.calac.gestor_archivos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Un modelo de datos simple para cada segmento de la ruta
data class PathSegment(val name: String, val path: String)

class BreadcrumbAdapter(
    private var segments: List<PathSegment>,
    private val onSegmentClickListener: (PathSegment) -> Unit
) : RecyclerView.Adapter<BreadcrumbAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.tv_breadcrumb_name)
        val separatorImageView: ImageView = view.findViewById(R.id.iv_separator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.breadcrumb_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val segment = segments[position]
        holder.nameTextView.text = segment.name

        // Oculta el separador (flecha) para el primer elemento ("Almacenamiento")
        holder.separatorImageView.visibility = if (position == 0) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener {
            onSegmentClickListener(segment)
        }
    }

    override fun getItemCount(): Int = segments.size

    fun updateData(newSegments: List<PathSegment>) {
        segments = newSegments
        notifyDataSetChanged()
    }
}