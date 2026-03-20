# Proyecto SGDH

Esta es una aplicación para Android que forma parte del sistema SGDH. La aplicación permite a los usuarios gestionar solicitudes de productos de manera eficiente.

## Características

*   Creación y gestión de solicitudes.
*   Visualización de productos.
*   Autenticación segura.
*   Notificaciones en tiempo real.

## Tecnologías Utilizadas

*   **Lenguaje:** Kotlin
*   **Arquitectura:** MVVM (Model-View-ViewModel)
*   **UI:** Vistas de Android (XML) con ViewBinding.
*   **Navegación:** Android Navigation Component.
*   **Red:** Retrofit y OkHttp para la comunicación con la API REST.
*   **Base de Datos Local:** Room.
*   **Carga de Imágenes:** Coil.
*   **Notificaciones:** Firebase Cloud Messaging.
*   **Autenticación biométrica:** BiometricPrompt.

## Configuración del Proyecto

1.  **Clonar el repositorio:**
    ```bash
    git clone https://github.com/JosueMM01/SGDH.git
    ```

2.  **Abrir en Android Studio:**
    Abre el proyecto con Android Studio. Gradle se encargará de descargar todas las dependencias necesarias.

3.  **Configurar Firebase:**
    Este proyecto utiliza Firebase. Para que funcione correctamente, necesitas tu propio archivo `google-services.json`.
    *   Ve a la [consola de Firebase](https://console.firebase.google.com/).
    *   Crea un nuevo proyecto o selecciona uno existente.
    *   Registra tu aplicación de Android con el nombre de paquete `com.example.sgdh`.
    *   Descarga el archivo `google-services.json`.
    *   Coloca el archivo `google-services.json` en el directorio `app/`.

4.  **Ejecutar la aplicación:**
    Construye y ejecuta la aplicación en un emulador o dispositivo físico.
