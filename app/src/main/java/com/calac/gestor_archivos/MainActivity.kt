package com.calac.gestor_archivos

import java.util.ArrayDeque
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Toast
import android.widget.EditText
import android.view.Menu // <-- AÑADE ESTA
import android.view.MenuItem // <-- AÑADE ESTA
import android.content.Context // <-- AÑADE ESTA
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.util.TypedValue // <-- AÑADE ESTA
import androidx.annotation.AttrRes // <-- AÑADE ESTA
import androidx.annotation.ColorInt // <-- AÑADE ESTA
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.ActivityNotFoundException
import android.graphics.Color
import androidx.core.content.FileProvider
import android.webkit.MimeTypeMap
import android.view.View // <-- AÑADE ESTA
import android.widget.Button // <-- AÑADE ESTA
import android.widget.TextView // <-- AÑADE ESTA
import androidx.cardview.widget.CardView // <-- AÑADE ESTA
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.text.format.Formatter

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE_READ_STORAGE = 101
    // Declarar las variables para el RecyclerView y el Adapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var fileAdapter: FileAdapter
    private var currentPath: String = Environment.getExternalStorageDirectory().absolutePath
    private val pathHistory = ArrayDeque<String>()
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private lateinit var fabAddFolder: FloatingActionButton
    private var pendingOperation: OperationType? = null
    private var fileToOperate: FileItem? = null
    private lateinit var pasteBar: CardView
    private lateinit var pasteTextView: TextView
    private lateinit var pasteButton: Button
    private lateinit var cancelPasteButton: Button

    enum class OperationType {
        COPY, MOVE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedTheme()
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        fabAddFolder = findViewById(R.id.fab_add_folder)
        fabAddFolder.setOnClickListener {
            showCreateFolderDialog()
        }

        pasteBar = findViewById(R.id.paste_bar)
        pasteTextView = findViewById(R.id.tv_paste_info)
        pasteButton = findViewById(R.id.btn_paste)
        cancelPasteButton = findViewById(R.id.btn_cancel_paste)

        cancelPasteButton.setOnClickListener {
            cancelPendingOperation()
        }

        pasteButton.setOnClickListener {
            performPasteOperation()
        }

        setupRecyclerView()
        setupBackButtonHandler() // Configurar el manejo del botón de "atrás"
        checkAndRequestPermissions()

        updatePasteBarUI()
    }

    // Este método infla (crea) el menú en la Toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // Este método se llama cuando se selecciona un ítem del menú
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_change_theme -> {
                showThemeSelectionDialog()
                true // Indica que hemos manejado el clic
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // --- FUNCIÓN PARA EL DIÁLOGO DE SELECCIÓN DE TEMA ---
    private fun showThemeSelectionDialog() {
        val themes = arrayOf("IPN Guinda", "ESCOM Azul")
        // Leer cuál es el tema actual (lo implementaremos)
        val currentThemeIndex = getCurrentThemeIndex()

        AlertDialog.Builder(this)
            .setTitle("Seleccionar Tema")
            .setSingleChoiceItems(themes, currentThemeIndex) { dialog, which ->
                // Guardar la nueva preferencia y recrear la actividad
                saveThemePreference(which)
                dialog.dismiss() // Cierra el diálogo
                recreate() // <-- IMPORTANTE: Reinicia la actividad para aplicar el tema
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- FUNCIONES PARA GUARDAR/LEER PREFERENCIAS (USANDO SharedPreferences) ---

    private fun saveThemePreference(themeIndex: Int) {
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("SelectedTheme", themeIndex)
            apply() // Guarda asíncronamente
        }
    }

    private fun getCurrentThemeIndex(): Int {
        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        // Devuelve 0 (IPN) si no hay nada guardado
        return sharedPref.getInt("SelectedTheme", 0)
    }

    // --- ACTUALIZA applySavedTheme() PARA USAR LA PREFERENCIA ---
    private fun applySavedTheme() {
        val themeIndex = getCurrentThemeIndex()
        val themeResId = if (themeIndex == 0) R.style.Theme_Gestor_Archivos_IPN else R.style.Theme_Gestor_Archivos_ESCOM
        setTheme(themeResId) // <-- Aplica el tema ANTES de que la UI se dibuje
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Ahora, al crear el adapter, le pasamos la función que manejará los clics
        fileAdapter = FileAdapter(
            emptyList(),
            { fileItem, itemView ->
                // Esta es la lógica que se ejecuta al hacer clic en un elemento
                if (fileItem.isDirectory) {
                    val highlightColor = getThemeColor(android.R.attr.colorControlHighlight)
                    itemView.setBackgroundColor(highlightColor)

                    // Espera 150ms ANTES de navegar
                    itemView.postDelayed({
                        // Reseteamos el fondo ANTES de navegar para evitar glitches
                        itemView.setBackgroundColor(Color.TRANSPARENT) // <-- LÍNEA IMPORTANTE AÑADIDA

                        pathHistory.push(currentPath)
                        navigateToDirectory(fileItem.path)
                    }, 150)

                    /*pathHistory.push(currentPath)
                    navigateToDirectory(fileItem.path)*/
                } else {
                    openFile(fileItem)
                }
            },
            { fileItem ->
                showFileOptionsDialog(fileItem)
            }
        )
        recyclerView.adapter = fileAdapter
    }

    /**
     * Obtiene un color definido en el tema actual.
     * @param attrRes El ID del atributo de color (ej. android.R.attr.colorControlHighlight).
     * @return El color resuelto como un Int.
     */
    @ColorInt
    private fun getThemeColor(@AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrRes, typedValue, true)
        return ContextCompat.getColor(this, typedValue.resourceId)
    }

    /**
     * Intenta abrir un archivo usando un Intent y el FileProvider.
     */
    private fun openFile(fileItem: FileItem) {
        val file = File(fileItem.path)

        // 1. Obtener la URI segura usando el FileProvider
        // La autoridad DEBE coincidir con la que pusiste en el AndroidManifest.xml
        val authority = "${packageName}.provider"
        val uri = FileProvider.getUriForFile(this, authority, file)

        // 2. Crear el Intent para ver el archivo
        val intent = Intent(Intent.ACTION_VIEW)

        // 3. Obtener el tipo MIME (ej. "image/jpeg", "application/pdf")
        val mimeType = getMimeType(file.name)

        intent.setDataAndType(uri, mimeType)

        // 4. Dar permisos temporales a la app que abrirá el archivo
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            // 5. Lanzar el Intent
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Manejar el caso donde no hay ninguna app instalada para abrir el archivo
            Toast.makeText(this, "No se encontró una aplicación para abrir este archivo.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Función de utilidad para obtener el tipo MIME de un archivo por su nombre.
     */
    private fun getMimeType(fileName: String): String? {
        // Obtener la extensión del archivo
        val extension = fileName.substringAfterLast('.', "")
        if (extension.isNotEmpty()) {
            // Buscar el tipo MIME correspondiente a esa extensión
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.getDefault()))
        }
        // Si no tiene extensión o no se encuentra, usamos un tipo genérico
        return "application/octet-stream"
    }

    /**
     * Muestra el diálogo de opciones (Renombrar, Eliminar, Detalles) para un archivo.
     */
    private fun showFileOptionsDialog(fileItem: FileItem) {
        // MODIFICACIÓN: Añade "Copiar" y "Mover"
        val options = arrayOf("Copiar", "Mover", "Renombrar", "Eliminar", "Detalles")

        AlertDialog.Builder(this)
            .setTitle(fileItem.name)
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Copiar
                        setPendingOperation(OperationType.COPY, fileItem)
                    }
                    1 -> { // Mover
                        setPendingOperation(OperationType.MOVE, fileItem)
                    }
                    2 -> { // Renombrar
                        showRenameDialog(fileItem)
                    }
                    3 -> { // Eliminar
                        deleteFileOrDirectory(fileItem)
                    }
                    4 -> { // Detalles
                        showDetailsDialog(fileItem)
                    }
                }
            }
            .show()
    }

    /**
     * Muestra un diálogo de confirmación y luego elimina un archivo o directorio.
     */
    private fun deleteFileOrDirectory(fileItem: FileItem) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar '${fileItem.name}'?")
            .setPositiveButton("Eliminar") { dialog, _ ->
                val file = File(fileItem.path)
                var deleted = false

                try {
                    // file.deleteRecursively() elimina carpetas con contenido
                    deleted = if (fileItem.isDirectory) file.deleteRecursively() else file.delete()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                if (deleted) {
                    Toast.makeText(this, "'${fileItem.name}' eliminado.", Toast.LENGTH_SHORT).show()
                    // Refresca la vista actual para mostrar que el archivo desapareció
                    navigateToDirectory(currentPath)
                } else {
                    Toast.makeText(this, "No se pudo eliminar '${fileItem.name}'.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Muestra un diálogo con un EditText para renombrar un archivo o carpeta.
     */
    private fun showRenameDialog(fileItem: FileItem) {
        // 1. Inflar (crear) la vista personalizada del diálogo
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rename, null)
        val editTextNewName = dialogView.findViewById<EditText>(R.id.et_new_name)

        // 2. Poner el nombre actual del archivo en el EditText
        editTextNewName.setText(fileItem.name)

        // 3. Construir el diálogo
        AlertDialog.Builder(this)
            .setTitle("Renombrar")
            .setView(dialogView) // 4. Asignar la vista personalizada
            .setPositiveButton("Renombrar") { dialog, _ ->
                val newName = editTextNewName.text.toString().trim()

                // 5. Validar que el nombre no esté vacío
                if (newName.isEmpty()) {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // 6. Realizar la operación de renombrado
                val oldFile = File(fileItem.path)

                // Obtenemos el directorio padre (ej. /storage/emulated/0/Download/)
                val parentDir = oldFile.parentFile

                if (parentDir != null) {
                    val newFile = File(parentDir, newName) // Creamos la nueva ruta

                    try {
                        if (oldFile.renameTo(newFile)) {
                            Toast.makeText(this, "Renombrado con éxito", Toast.LENGTH_SHORT).show()
                            // 7. Refrescar la lista para ver el cambio
                            navigateToDirectory(currentPath)
                        } else {
                            Toast.makeText(this, "Error al renombrar", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Muestra un diálogo con los detalles de un archivo o carpeta.
     */
    private fun showDetailsDialog(fileItem: FileItem) {
        // 1. Formatear la fecha de modificación
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val lastModified = sdf.format(Date(fileItem.modifiedDate))

        // 2. Formatear el tamaño
        // Usamos Formatter.formatShortFileSize para obtener un string legible (ej. "1.2 MB")
        val size = if (fileItem.isDirectory) {
            "---" // No calculamos el tamaño de la carpeta por ahora
        } else {
            Formatter.formatShortFileSize(this, fileItem.sizeInBytes)
        }

        // 3. Construir el mensaje del diálogo
        val detailsMessage = StringBuilder().apply {
            append("Ruta:\n${fileItem.path}\n\n")
            append("Tamaño:\n$size\n\n")
            append("Última modificación:\n$lastModified")
        }.toString()

        // 4. Mostrar el diálogo
        AlertDialog.Builder(this)
            .setTitle(fileItem.name)
            .setMessage(detailsMessage)
            .setPositiveButton("Cerrar", null) // Un simple botón para cerrar
            .show()
    }

    /**
     * Muestra un diálogo para crear una nueva carpeta.
     */
    private fun showCreateFolderDialog() {
        // Reutilizamos el mismo layout del diálogo de renombrar
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rename, null)
        val editTextNewName = dialogView.findViewById<EditText>(R.id.et_new_name)

        // Cambiamos el texto de ayuda (hint)
        editTextNewName.hint = "Nombre de la carpeta"

        AlertDialog.Builder(this)
            .setTitle("Crear Nueva Carpeta")
            .setView(dialogView)
            .setPositiveButton("Crear") { dialog, _ ->
                val newFolderName = editTextNewName.text.toString().trim()

                if (newFolderName.isEmpty()) {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Crear la ruta del nuevo directorio
                val newFolder = File(currentPath, newFolderName)

                try {
                    // Intentar crear el directorio
                    if (newFolder.mkdir()) { // mkdir() crea un solo directorio
                        Toast.makeText(this, "Carpeta creada", Toast.LENGTH_SHORT).show()
                        // Refrescar la lista para ver la nueva carpeta
                        navigateToDirectory(currentPath)
                    } else {
                        Toast.makeText(this, "Error al crear la carpeta", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Establece la operación pendiente (Copiar/Mover) y actualiza la UI.
     */
    private fun setPendingOperation(operation: OperationType, file: FileItem) {
        pendingOperation = operation
        fileToOperate = file
        updatePasteBarUI()
    }

    /**
     * Cancela la operación pendiente y actualiza la UI.
     */
    private fun cancelPendingOperation() {
        pendingOperation = null
        fileToOperate = null
        updatePasteBarUI()
    }

    /**
     * Muestra u oculta la barra de pegado y el FAB según el estado.
     */
    private fun updatePasteBarUI() {
        if (pendingOperation != null && fileToOperate != null) {
            // Hay una operación pendiente: Mostrar barra, ocultar FAB
            pasteBar.visibility = View.VISIBLE
            val operationText = if (pendingOperation == OperationType.COPY) "Copiando" else "Moviendo"
            pasteTextView.text = "$operationText: ${fileToOperate!!.name}"

            // Ocultamos el FAB para que no se superponga
            fabAddFolder.hide()

        } else {
            // No hay operación: Ocultar barra, mostrar FAB
            pasteBar.visibility = View.GONE
            fabAddFolder.show()
        }
    }

    /**
     * Ejecuta la operación de Copiar o Mover.
     */
    private fun performPasteOperation() {
        // 1. Doble chequeo de seguridad
        if (fileToOperate == null || pendingOperation == null) {
            cancelPendingOperation()
            return
        }

        val sourceFile = File(fileToOperate!!.path)
        val destinationDir = File(currentPath)

        // 2. Crear el archivo/carpeta de destino
        val destinationFile = File(destinationDir, sourceFile.name)

        // 3. Prevenir sobreescritura o errores
        if (destinationFile.exists()) {
            Toast.makeText(this, "Ya existe un archivo con ese nombre aquí.", Toast.LENGTH_SHORT).show()
            return
        }

        // 4. Prevenir copiar/mover algo dentro de sí mismo o en el mismo lugar
        if (sourceFile.path == destinationFile.path || sourceFile.parent == destinationDir.path && pendingOperation == OperationType.MOVE) {
            Toast.makeText(this, "La ubicación de origen y destino es la misma.", Toast.LENGTH_SHORT).show()
            cancelPendingOperation()
            return
        }


        try {
            var success = false
            if (pendingOperation == OperationType.COPY) {
                // 5. Copiar (recursivamente para carpetas)
                sourceFile.copyRecursively(destinationFile, overwrite = false)
                success = true
                Toast.makeText(this, "Copiado con éxito", Toast.LENGTH_SHORT).show()

            } else if (pendingOperation == OperationType.MOVE) {
                // 6. Mover (copiando y luego borrando, es más seguro)
                sourceFile.copyRecursively(destinationFile, overwrite = false)
                sourceFile.deleteRecursively() // Borra el original
                success = true
                Toast.makeText(this, "Movido con éxito", Toast.LENGTH_SHORT).show()
            }

            if (success) {
                // 7. Refrescar la vista
                navigateToDirectory(currentPath)
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            // 8. Limpiar la operación
            cancelPendingOperation()
        }
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