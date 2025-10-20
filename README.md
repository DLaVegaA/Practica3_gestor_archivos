# P3 - Gestor de Archivos Básico para Android

Este proyecto consiste en una aplicación nativa para Android que funciona como un gestor de archivos básico, permitiendo al usuario explorar y administrar el almacenamiento interno y externo de su dispositivo.

---

## ## Descripción de la Aplicación

El Gestor de Archivos ofrece las siguientes funcionalidades:

* **Exploración de Directorios:** Navega por la estructura de carpetas del almacenamiento interno y externo.
* **Visualización Jerárquica:** Muestra carpetas y archivos en una lista clara, con iconos diferenciados por tipo (carpeta, imagen, audio, video, PDF, texto, etc.).
* **Navegación Intuitiva:** Incluye una barra de ruta ("breadcrumbs") para facilitar la navegación entre directorios padres.
* **Gestión Básica:** Permite crear nuevas carpetas, renombrar, copiar, mover y eliminar archivos y carpetas existentes.
* **Apertura de Archivos:**
    * Abre archivos de texto (`.txt`, `.md`, `.log`, `.json`, `.xml`, etc.) en un visor interno.
    * Abre archivos de imagen (`.jpg`, `.png`, etc.) en un visor interno con capacidad de zoom y desplazamiento.
    * Utiliza `Intents` para abrir otros tipos de archivo con las aplicaciones adecuadas instaladas en el sistema.
* **Personalización:**
    * Ofrece dos temas de color: Guinda (IPN) y Azul (ESCOM).
    * Permite seleccionar el modo de visualización (Claro, Oscuro o Automático según el sistema).
    * Permite cambiar entre vista de lista y vista de cuadrícula.
* **Funciones Adicionales:**
    * **Búsqueda:** Filtra la lista actual por nombre de archivo.
    * **Favoritos:** Marca archivos o carpetas como favoritos para un acceso rápido (persistencia mediante base de datos Room).
    * **Historial:** Guarda automáticamente los últimos archivos abiertos (persistencia mediante base de datos Room) en una sección colapsable.
* **Rendimiento:** Utiliza caché de miniaturas (gestionado por Glide) para una carga más rápida de las vistas previas de imágenes.

---

## ## Requisitos del Sistema

* **Android Studio:** [Indica aquí la versión de Android Studio que usaste, ej: Hedgehog | 2023.1.1 o superior]
* **Gradle:** [Indica aquí la versión de Gradle, ej: 8.0 o superior]
* **API Mínima:** [Indica aquí el `minSdkVersion` definido en tu `build.gradle.kts (app)`, ej: API 24 (Android 7.0 Nougat)]

---

## ## Instrucciones de Instalación

1.  **Clonar o Descargar:** Obtén el código fuente del repositorio.
    ```bash
    git clone [URL_DEL_REPOSITORIO]
    ```
    O descarga el archivo ZIP y descomprímelo.
2.  **Abrir en Android Studio:**
    * Abre Android Studio.
    * Selecciona "Open an Existing Project".
    * Navega hasta la carpeta del proyecto clonado/descomprimido y selecciónala.
3.  **Sincronizar Gradle:** Espera a que Android Studio descargue las dependencias necesarias (puede tardar unos minutos).
4.  **Ejecutar:** Conecta un dispositivo Android o inicia un emulador y ejecuta la aplicación desde Android Studio (Run > Run 'app').

---

## ## Permisos Requeridos y Justificación

La aplicación requiere los siguientes permisos, declarados en `AndroidManifest.xml`:

1.  **`android.permission.READ_EXTERNAL_STORAGE`**:
    * **Justificación:** Necesario en versiones de Android 9 (Pie) y anteriores para leer archivos y carpetas del almacenamiento externo. Permite a la app listar el contenido de los directorios.
2.  **`android.permission.WRITE_EXTERNAL_STORAGE` (con `maxSdkVersion="28"`)**:
    * **Justificación:** Necesario en versiones de Android 9 (Pie) y anteriores para realizar operaciones de escritura como crear carpetas, renombrar, mover o eliminar archivos/carpetas en el almacenamiento externo. Se limita a `maxSdkVersion="28"` porque en versiones posteriores se usa `MANAGE_EXTERNAL_STORAGE`.
3.  **`android.permission.MANAGE_EXTERNAL_STORAGE`**:
    * **Justificación:** Necesario en Android 11 (API 30) y versiones posteriores para obtener acceso completo de gestión al almacenamiento externo. Este permiso es esencial para que la aplicación funcione como un gestor de archivos, permitiendo leer, escribir, crear, renombrar y eliminar archivos/carpetas en cualquier directorio accesible por el usuario (fuera de los directorios privados de otras apps). El usuario debe conceder este permiso manualmente desde la configuración del sistema.

Estos permisos son fundamentales para la funcionalidad principal de la aplicación como gestor de archivos. Sin ellos, la aplicación no podría acceder ni modificar el contenido del almacenamiento del dispositivo.

---

## ## Capturas de Pantalla

![WhatsApp Image 2025-10-19 at 8 09 31 PM](https://github.com/user-attachments/assets/9436e3e4-5047-4dbb-a73a-0c65b28384b4)
![WhatsApp Image 2025-10-19 at 8 09 31 PM (1)](https://github.com/user-attachments/assets/99e2c047-ff49-4489-a72b-4bf4462aa2fe)
![WhatsApp Image 2025-10-19 at 1 27 41 AM](https://github.com/user-attachments/assets/220ed91b-4ea3-455d-8ab0-deede1da653d)
![WhatsApp Image 2025-10-19 at 8 08 54 PM](https://github.com/user-attachments/assets/188d56c0-6a9f-4019-a0fa-81d3a089bef5)
![WhatsApp Image 2025-10-19 at 8 08 54 PM (1)](https://github.com/user-attachments/assets/d1407460-2b9c-4af4-9483-48a9cffe5216)
![WhatsApp Image 2025-10-19 at 8 08 53 PM](https://github.com/user-attachments/assets/b41e6fd2-4fcc-4f07-a593-fdd1f14e2a3b)
![WhatsApp Image 2025-10-19 at 8 08 53 PM (4)](https://github.com/user-attachments/assets/690df213-0773-4ad6-ac05-596b3ffcd3a4)
![WhatsApp Image 2025-10-19 at 8 08 53 PM (3)](https://github.com/user-attachments/assets/0ee89059-c629-46bd-8c1b-9e10ba5a1cf8)
![WhatsApp Image 2025-10-19 at 8 08 53 PM (2)](https://github.com/user-attachments/assets/960f0496-e298-4e1b-9b7e-2f4a6d34ab55)
![WhatsApp Image 2025-10-19 at 8 08 53 PM (1)](https://github.com/user-attachments/assets/5e444ffb-5f66-43ba-8811-ba8dff1c32b0)
![WhatsApp Image 2025-10-19 at 8 08 52 PM](https://github.com/user-attachments/assets/633b8ff1-cb60-45c5-86a7-1198996f911e)
![WhatsApp Image 2025-10-19 at 8 08 52 PM (3)](https://github.com/user-attachments/assets/24d05305-2a48-4230-a6b6-6511ea301d61)
![WhatsApp Image 2025-10-19 at 8 08 52 PM (2)](https://github.com/user-attachments/assets/d2e80639-aade-4f40-8b03-3e99f6b5c089)
![WhatsApp Image 2025-10-19 at 8 08 52 PM (1)](https://github.com/user-attachments/assets/ad1ec258-03bf-4e36-900d-cede482259e0)
![WhatsApp Image 2025-10-19 at 8 08 51 PM](https://github.com/user-attachments/assets/ef52b5e8-5c4a-4aae-8ac8-ffaeb9625488)
![WhatsApp Image 2025-10-19 at 8 08 51 PM (3)](https://github.com/user-attachments/assets/b492a0f0-d9e9-4b41-8158-23cb696508bd)
![WhatsApp Image 2025-10-19 at 8 08 51 PM (2)](https://github.com/user-attachments/assets/264e3def-517f-4a1f-bd23-334e2767d0f6)
![WhatsApp Image 2025-10-19 at 8 08 51 PM (1)](https://github.com/user-attachments/assets/5e31672e-dc20-4dcd-aefa-53738ba9523c)

---
