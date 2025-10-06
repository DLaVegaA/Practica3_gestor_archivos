package com.calac.gestor_archivos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_READ_STORAGE = 101

    // Declarar las variables para el RecyclerView y el Adapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter

    private var currentPath: String = Environment.getExternalStorageDirectory().absolutePath
    private val pathHistory = ArrayDeque<String>()

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupRecyclerView()
        setupBackButtonHandler() // Configurar el manejo del botón de "atrás"

        checkAndRequestPermissions()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Ahora, al crear el adapter, le pasamos la función que manejará los clics
        fileAdapter = FileAdapter(emptyList()) { fileItem ->
            // Esta es la lógica que se ejecuta al hacer clic en un elemento
            if (fileItem.isDirectory) {
                // Si es un directorio, navegamos hacia él
                pathHistory.push(currentPath) // Guardamos la ruta actual en el historial
                navigateToDirectory(fileItem.path)
            } else {
                // Si es un archivo, por ahora mostramos un mensaje
                Toast.makeText(this, "Archivo: ${fileItem.name}", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = fileAdapter
    }

    private fun setupBackButtonHandler() {
        // CORRECCIÓN CLAVE: Se asigna el objeto a la variable de la clase 'onBackPressedCallback'
        onBackPressedCallback = object : OnBackPressedCallback(false) { // Inicia deshabilitado
            override fun handleOnBackPressed() {
                if (pathHistory.isNotEmpty()) {
                    val previousPath = pathHistory.pop()
                    navigateToDirectory(previousPath) // Llamada corregida
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun navigateToDirectory(path: String) {
        currentPath = path
        val files = getFilesFromPath(path)
        fileAdapter.updateData(files)
        // MODIFICADO: Solo se actualiza el estado del callback existente
        onBackPressedCallback.isEnabled = pathHistory.isNotEmpty()
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                navigateToDirectory(currentPath)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.fromParts("package", packageName, null)
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            } else {
                navigateToDirectory(currentPath)
            }
        } else { // Android 10 y anteriores
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_READ_STORAGE)
            } else {
                navigateToDirectory(currentPath)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                navigateToDirectory(currentPath)
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado.", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Modificamos esta función para que devuelva una lista de 'FileItem' en lugar de 'File'.
     * Esto conecta la lógica de archivos con el modelo de datos que creaste.
     */
    private fun getFilesFromPath(path: String): List<FileItem> {
        val file = File(path)
        val files = file.listFiles()

        if (files == null) {
            Toast.makeText(this, "No se puede acceder a la ruta: $path", Toast.LENGTH_SHORT).show()
            return emptyList()
        }

        return files.map {
            FileItem(
                name = it.name,
                path = it.absolutePath,
                isDirectory = it.isDirectory,
                sizeInBytes = if (it.isFile) it.length() else 0L,
                modifiedDate = it.lastModified()
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) // Ordena: carpetas primero, luego alfabéticamente
    }
}