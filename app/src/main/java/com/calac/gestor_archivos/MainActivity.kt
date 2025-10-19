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
import androidx.appcompat.app.AppCompatDelegate // <-- AÑADE ESTA
import android.content.Context // <-- AÑADE ESTA
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import android.util.TypedValue // <-- AÑADE ESTA
import androidx.annotation.AttrRes // <-- AÑADE ESTA
import androidx.annotation.ColorInt // <-- AÑADE ESTA
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
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
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

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
    private lateinit var breadcrumbRecyclerView: RecyclerView
    private lateinit var breadcrumbAdapter: BreadcrumbAdapter
    private val rootPath = Environment.getExternalStorageDirectory().absolutePath
    private var viewModeMenuItem: MenuItem? = null
    private var currentViewMode = 0 // 0 = Lista, 1 = Cuadrícula
    // Inicialización 'lazy' (perezosa) de la base de datos
    private val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    private val favoriteDao: FavoriteDao by lazy { database.favoriteDao() }
    private val recentFileDao: RecentFileDao by lazy { database.recentFileDao() } // <-- ADD THIS

    // Un set para guardar las rutas de los favoritos cargados
    private var favoritePaths = setOf<String>()
    private lateinit var recentFilesRecyclerView: RecyclerView
    private lateinit var recentFilesAdapter: RecentFilesAdapter
    private lateinit var recentFilesHeader: View
    private lateinit var recentFilesToggleIcon: ImageView
    private var isRecentsExpanded = false // Estado de la sección

    enum class OperationType {
        COPY, MOVE
    }

    companion object {
        const val PREFS_NAME = "AppPreferences"
        const val PREF_KEY_THEME = "SelectedTheme"
        const val PREF_KEY_MODE = "SelectedMode"
        const val PREF_KEY_VIEW_MODE = "ViewMode" // <-- NUEVA CONSTANTE
        const val VIEW_MODE_LIST = 0
        const val VIEW_MODE_GRID = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedNightMode()
        applySavedTheme()

        setContentView(R.layout.activity_main)

        currentViewMode = getCurrentViewMode()

        // Inicialización de Recientes
        recentFilesRecyclerView = findViewById(R.id.recentFilesRecyclerView)
        recentFilesHeader = findViewById(R.id.recent_files_header)
        recentFilesToggleIcon = findViewById(R.id.iv_recent_files_toggle)
        setupRecentFilesView() // Nueva función de setup

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
        setupBreadcrumbView()
        setupBackButtonHandler() // Configurar el manejo del botón de "atrás"
        checkAndRequestPermissions()

        updatePasteBarUI()
        loadAndDisplayRecentFiles()
    }

    private fun setupRecentFilesView() {
        recentFilesRecyclerView.layoutManager = LinearLayoutManager(this)
        recentFilesAdapter = RecentFilesAdapter(emptyList()) { recentFile, itemView ->
            // Lógica al hacer clic en un archivo reciente
            val file = File(recentFile.path)
            if (file.exists()) {
                // Reutilizamos la lógica de FileItem (creando uno temporal)
                val fileItem = FileItem(
                    recentFile.name,
                    recentFile.path,
                    false, // Sabemos que no es directorio si está en recientes
                    file.length(),
                    file.lastModified()
                )
                openFile(fileItem) // Llama a tu función openFile existente
            } else {
                Toast.makeText(this, "El archivo ya no existe", Toast.LENGTH_SHORT).show()
                // (Opcional: Borrarlo del historial si ya no existe)
                // removeRecentFile(recentFile)
            }
        }
        recentFilesRecyclerView.adapter = recentFilesAdapter

        // Lógica para expandir/colapsar
        recentFilesHeader.setOnClickListener {
            toggleRecentsSection()
        }
        // Inicializa el estado visual
        updateRecentsToggleIcon()
    }

    private fun setupBreadcrumbView() {
        breadcrumbRecyclerView = findViewById(R.id.breadcrumbRecyclerView)
        // El layout manager ya está en el XML, pero podemos configurarlo aquí si queremos
        // breadcrumbRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        breadcrumbAdapter = BreadcrumbAdapter(emptyList()) { pathSegment ->
            // Lógica de clic en un breadcrumb

            // 1. No hacer nada si se pulsa la carpeta en la que ya estamos
            if (pathSegment.path == currentPath) return@BreadcrumbAdapter

            // 2. Limpiar el historial de "vuelta atrás" hasta ese punto
            // Ej: Si estamos en A > B > C > D y pulsamos "B"
            // El historial (A, B, C) debe limpiarse hasta "A"
            while (pathHistory.isNotEmpty() && pathHistory.peek() != pathSegment.path) {
                if (pathHistory.peek() == rootPath && pathSegment.path != rootPath) {
                    // Caso especial: no quitar la raíz si no vamos a ella
                    break
                }
                pathHistory.pop()
                if (pathHistory.isEmpty()) break
            }

            // 3. Navegar
            navigateToDirectory(pathSegment.path)
        }
        breadcrumbRecyclerView.adapter = breadcrumbAdapter
    }

    // Este método infla (crea) el menú en la Toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        // Configuración del ícono de Vista (Lista/Cuadrícula)
        viewModeMenuItem = menu?.findItem(R.id.action_toggle_view)
        updateViewModeIcon()

        // --- Lógica de Búsqueda ---
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        // Configura el listener para el texto de búsqueda
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            // No necesitamos hacer nada cuando el usuario presiona "Enter"
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            // Esta función se llama CADA VEZ que el usuario escribe una letra
            override fun onQueryTextChange(newText: String?): Boolean {
                // Pasa el texto de búsqueda al adapter para que filtre
                fileAdapter.filter(newText.orEmpty())
                return true
            }
        })

        // (Opcional pero recomendado) Resetea el filtro cuando se cierra la búsqueda
        searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true // Permitir que se expanda
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                // Cuando la barra de búsqueda se cierra, limpia el filtro
                fileAdapter.filter("")
                return true // Permitir que se cierre
            }
        })

        return true
    }

    // Este método se llama cuando se selecciona un ítem del menú
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_change_theme -> {
                showThemeSelectionDialog()
                true // Indica que hemos manejado el clic
            }
            R.id.action_change_mode -> {
                showModeSelectionDialog()
                true
            }
            R.id.action_toggle_view -> {
                toggleViewMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showModeSelectionDialog() {
        val modes = arrayOf("Automático (Sistema)", "Claro", "Oscuro")
        val currentModeIndex = getCurrentModeIndex() // 0=Auto, 1=Claro, 2=Oscuro

        AlertDialog.Builder(this)
            .setTitle("Seleccionar Modo")
            .setSingleChoiceItems(modes, currentModeIndex) { dialog, which ->
                saveModePreference(which)
                dialog.dismiss()
                recreate() // Reinicia la app para aplicar el modo
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt(PREF_KEY_THEME, themeIndex)
            apply()
        }
    }

    private fun getCurrentThemeIndex(): Int {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getInt(PREF_KEY_THEME, 0) // 0 = IPN
    }

    private fun saveModePreference(modeIndex: Int) {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt(PREF_KEY_MODE, modeIndex)
            apply()
        }
    }

    private fun getCurrentModeIndex(): Int {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getInt(PREF_KEY_MODE, 0) // 0 = Automático
    }

    private fun applySavedNightMode() {
        val modeIndex = getCurrentModeIndex()
        val mode = when (modeIndex) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO  // 1 = Forzar Claro
            2 -> AppCompatDelegate.MODE_NIGHT_YES // 2 = Forzar Oscuro
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM // 0 = Automático
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    // --- ACTUALIZA applySavedTheme() PARA USAR LA PREFERENCIA ---
    private fun applySavedTheme() {
        val themeIndex = getCurrentThemeIndex()
        val themeResId = if (themeIndex == 0) R.style.Theme_Gestor_Archivos_IPN else R.style.Theme_Gestor_Archivos_ESCOM
        setTheme(themeResId) // <-- Aplica el tema ANTES de que la UI se dibuje
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        //recyclerView.layoutManager = LinearLayoutManager(this)
        applyLayoutManager()

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
     * Cambia entre modo Lista y Cuadrícula.
     */
    private fun toggleViewMode() {
        // Cambia el modo (si es 0 se vuelve 1, si es 1 se vuelve 0)
        currentViewMode = if (currentViewMode == VIEW_MODE_LIST) VIEW_MODE_GRID else VIEW_MODE_LIST
        // Guarda la nueva preferencia
        saveViewMode(currentViewMode)
        // Aplica el cambio de layout
        applyLayoutManager()
        // Actualiza el ícono del menú
        updateViewModeIcon()
    }

    /**
     * Aplica el LinearLayoutManager o GridLayoutManager al RecyclerView.
     */
    private fun applyLayoutManager() {
        if (currentViewMode == VIEW_MODE_GRID) {
            // Modo Cuadrícula (ej. 3 columnas)
            recyclerView.layoutManager = GridLayoutManager(this, 3)
        } else {
            // Modo Lista
            recyclerView.layoutManager = LinearLayoutManager(this)
        }
    }

    /**
     * Actualiza el ícono del menú para mostrar la acción opuesta.
     */
    private fun updateViewModeIcon() {
        if (currentViewMode == VIEW_MODE_GRID) {
            // Si estamos en Cuadrícula, el botón debe mostrar "Cambiar a Lista"
            viewModeMenuItem?.setIcon(R.drawable.ic_view_list)
        } else {
            // Si estamos en Lista, el botón debe mostrar "Cambiar a Cuadrícula"
            viewModeMenuItem?.setIcon(R.drawable.ic_view_grid)
        }
    }

    /**
     * Guarda la preferencia de vista (Lista/Cuadrícula).
     */
    private fun saveViewMode(mode: Int) {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt(PREF_KEY_VIEW_MODE, mode)
            apply()
        }
    }

    /**
     * Obtiene la preferencia de vista guardada.
     */
    private fun getCurrentViewMode(): Int {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getInt(PREF_KEY_VIEW_MODE, VIEW_MODE_LIST) // Lista por defecto
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
            // If opening was successful, add it to recents
            addRecentFile(fileItem)
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
        // 1. Comprueba si el archivo ya es un favorito
        val isFavorite = favoritePaths.contains(fileItem.path)
        val favoriteOptionText = if (isFavorite) {
            "Quitar de Favoritos"
        } else {
            "Añadir a Favoritos"
        }

        // 2. Crea la lista de opciones dinámicamente
        val options = mutableListOf(
            favoriteOptionText,
            "Copiar",
            "Mover",
            "Renombrar",
            "Eliminar",
            "Detalles"
        )

        // 3. Muestra el diálogo
        AlertDialog.Builder(this)
            .setTitle(fileItem.name)
            .setItems(options.toTypedArray()) { dialog, which ->
                // 4. Maneja el clic basado en el TEXTO de la opción
                when (options[which]) {
                    "Añadir a Favoritos" -> addFavorite(fileItem)
                    "Quitar de Favoritos" -> removeFavorite(fileItem)
                    "Copiar" -> setPendingOperation(OperationType.COPY, fileItem)
                    "Mover" -> setPendingOperation(OperationType.MOVE, fileItem)
                    "Renombrar" -> showRenameDialog(fileItem)
                    "Eliminar" -> deleteFileOrDirectory(fileItem)
                    "Detalles" -> showDetailsDialog(fileItem)
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
        // Carga los favoritos y actualiza las estrellas en el adapter
        loadFavorites()
        // Actualiza los breadcrumbs
        val segments = parsePathToSegments(path)
        breadcrumbAdapter.updateData(segments)
        // Mueve la barra de breadcrumbs al final para mostrar el último elemento
        breadcrumbRecyclerView.scrollToPosition(segments.size - 1)
        onBackPressedCallback.isEnabled = pathHistory.isNotEmpty()
    }

    /**
     * Convierte una ruta absoluta (ej. /storage/emulated/0/Download/Docs)
     * en una lista de PathSegment para el adapter.
     */
    private fun parsePathToSegments(fullPath: String): List<PathSegment> {
        val segments = mutableListOf<PathSegment>()
        // 1. Añadir el segmento Raíz
        segments.add(PathSegment("Almacenamiento", rootPath))

        // 2. Comprobar si estamos en una subcarpeta
        if (fullPath.length > rootPath.length) {
            // Obtener la parte relativa (ej. "/Download/Docs")
            val relativePath = fullPath.substring(rootPath.length + 1)

            // Dividir la ruta relativa por "/"
            val folders = relativePath.split('/')

            var currentSegmentPath = rootPath
            for (folderName in folders) {
                currentSegmentPath = "$currentSegmentPath/$folderName"
                segments.add(PathSegment(folderName, currentSegmentPath))
            }
        }
        return segments
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

    /**
     * Carga la lista de favoritos desde la BD (en un hilo secundario)
     * y actualiza el adapter.
     */
    private fun loadFavorites() {
        // lifecycleScope.launch inicia una coroutine (hilo secundario)
        lifecycleScope.launch {
            try {
                // Obtenemos todos los favoritos de la base de datos
                val favorites = favoriteDao.getAllFavorites()
                // Extraemos solo las rutas y las guardamos en nuestro Set
                favoritePaths = favorites.map { it.path }.toSet()
                // Le pasamos el Set al adapter para que muestre las estrellas
                fileAdapter.updateFavorites(favoritePaths)
            } catch (e: Exception) {
                // Manejar error de base de datos si ocurre
                Toast.makeText(this@MainActivity, "Error al cargar favoritos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Añade un archivo a la base de datos de favoritos.
     */
    private fun addFavorite(fileItem: FileItem) {
        lifecycleScope.launch {
            try {
                // Crea el objeto 'Favorite' que definimos en Favorite.kt
                val favorite = Favorite(
                    path = fileItem.path,
                    name = fileItem.name,
                    isDirectory = fileItem.isDirectory
                )
                // Llama a la función 'suspend' del DAO para insertarlo
                favoriteDao.insert(favorite)
                // Vuelve a cargar la lista para refrescar la estrella
                loadFavorites()
                Toast.makeText(this@MainActivity, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al añadir", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Elimina un archivo de la base de datos de favoritos.
     */
    private fun removeFavorite(fileItem: FileItem) {
        lifecycleScope.launch {
            try {
                // Para borrar, solo necesitamos un objeto 'Favorite' con la PrimaryKey (la ruta)
                val favorite = Favorite(
                    path = fileItem.path,
                    name = fileItem.name,
                    isDirectory = fileItem.isDirectory
                )
                // Llama a la función 'suspend' del DAO para borrarlo
                favoriteDao.delete(favorite)
                // Vuelve a cargar la lista para refrescar la estrella
                loadFavorites()
                Toast.makeText(this@MainActivity, "Quitado de favoritos", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al quitar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Adds a file to the recent files database.
     */
    private fun addRecentFile(fileItem: FileItem) {
        lifecycleScope.launch {
            try {
                // Create the RecentFile object with current time
                val recentFile = RecentFile(
                    path = fileItem.path,
                    name = fileItem.name,
                    lastOpenedTimestamp = System.currentTimeMillis() // Get current time in milliseconds
                )
                // Insert/Update the record in the database
                recentFileDao.insertOrUpdate(recentFile)

                // Optional: Keep the history trimmed to, say, 20 items
                recentFileDao.trimHistory(limit = 20)

            } catch (e: Exception) {
                // Log the error or show a silent notification if needed
                // Avoid showing a toast here as it might be annoying
                // Log.e("MainActivity", "Error adding recent file", e)
            }
        }
    }

    /**
     * (Placeholder for future use) Loads recent files from the database.
     * We'll need this later if we want to display a "Recent Files" list.
     */
    private suspend fun loadRecentFiles(): List<RecentFile> {
        return try {
            recentFileDao.getAllRecents()
        } catch (e: Exception) {
            // Log error
            emptyList()
        }
    }

    private fun loadAndDisplayRecentFiles() {
        lifecycleScope.launch {
            val recents = loadRecentFiles() // Llama a tu función suspendida existente
            recentFilesAdapter.updateData(recents)
            // Opcional: Ocultar la cabecera si no hay recientes
            recentFilesHeader.visibility = if (recents.isEmpty()) View.GONE else View.VISIBLE
            // Asegurarse de que la lista esté colapsada si no hay ítems
            if (recents.isEmpty()) {
                isRecentsExpanded = false
                recentFilesRecyclerView.visibility = View.GONE
                updateRecentsToggleIcon()
            }
        }
    }

    private fun toggleRecentsSection() {
        isRecentsExpanded = !isRecentsExpanded
        recentFilesRecyclerView.visibility = if (isRecentsExpanded) View.VISIBLE else View.GONE
        updateRecentsToggleIcon()
    }

    private fun updateRecentsToggleIcon() {
        val iconRes = if (isRecentsExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more
        recentFilesToggleIcon.setImageResource(iconRes)
    }
}