package com.calac.gestor_archivos

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide // Importa Glide
import com.github.chrisbanes.photoview.PhotoView // Importa PhotoView
import java.io.File

class ImageViewerActivity : AppCompatActivity() {

    // --- ▼▼▼ DEFINE LA CONSTANTE EXTRA ▼▼▼ ---
    companion object {
        const val EXTRA_FILE_PATH = "com.calac.gestor_archivos.IMAGE_FILE_PATH"
    }
    // --- ▲▲▲ FIN ▲▲▲ ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        val toolbar: Toolbar = findViewById(R.id.image_viewer_toolbar)
        val photoView: PhotoView = findViewById(R.id.photo_view) // Encuentra el PhotoView

        // Obtiene la ruta del archivo desde el Intent
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)

        if (filePath == null) {
            Toast.makeText(this, "Error: No se especificó la imagen.", Toast.LENGTH_LONG).show()
            finish() // Cierra si no hay ruta
            return
        }

        val file = File(filePath)

        // Configura la Toolbar
        toolbar.title = file.name
        toolbar.setNavigationOnClickListener {
            finish() // Cierra la actividad al pulsar atrás
        }

        // --- Carga la imagen usando Glide en el PhotoView ---
        Glide.with(this) // Contexto
            .load(file) // La fuente es el archivo (Glide maneja File objects)
            .error(android.R.drawable.ic_menu_report_image) // Ícono si falla la carga (opcional)
            .into(photoView) // El destino es nuestro PhotoView
    }
}