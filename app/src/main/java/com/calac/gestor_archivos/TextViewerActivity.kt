package com.calac.gestor_archivos

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import kotlinx.coroutines.Dispatchers // Import Dispatchers
import kotlinx.coroutines.launch      // Import launch
import kotlinx.coroutines.withContext // Import withContext
import java.io.File
import java.io.IOException

class TextViewerActivity : AppCompatActivity() {

    // Key for passing the file path via Intent
    companion object {
        const val EXTRA_FILE_PATH = "com.calac.gestor_archivos.FILE_PATH"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_viewer)

        val toolbar: Toolbar = findViewById(R.id.text_viewer_toolbar)
        val fileContentTextView: TextView = findViewById(R.id.tv_file_content)

        // Get the file path from the Intent
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)

        if (filePath == null) {
            Toast.makeText(this, "Error: No se especificó el archivo.", Toast.LENGTH_LONG).show()
            finish() // Close the activity if no path is provided
            return
        }

        val file = File(filePath)

        // Set up the Toolbar
        toolbar.title = file.name
        toolbar.setNavigationOnClickListener {
            finish() // Close activity when back arrow is pressed
        }

        // --- Read and Display File Content (using Coroutines) ---
        // Use lifecycleScope to run this in the background
        lifecycleScope.launch(Dispatchers.IO) { // Switch to IO thread for file reading
            val contentResult = readFileContent(file)

            // Switch back to the Main thread to update the UI
            withContext(Dispatchers.Main) {
                if (contentResult.isSuccess) {
                    fileContentTextView.text = contentResult.getOrNull()
                } else {
                    fileContentTextView.text = "Error al leer el archivo:\n${contentResult.exceptionOrNull()?.message}"
                    Toast.makeText(this@TextViewerActivity, "Error al leer el archivo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Helper function to read file content safely
    private suspend fun readFileContent(file: File): Result<String> {
        // Run the blocking file read operation on the IO dispatcher
        return withContext(Dispatchers.IO) {
            try {
                // Read all lines and join them with newline characters
                Result.success(file.readLines().joinToString("\n"))
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: OutOfMemoryError) {
                Result.failure(IOException("El archivo es demasiado grande para abrirlo."))
            }
        }
    }
}