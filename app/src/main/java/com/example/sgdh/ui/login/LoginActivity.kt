package com.example.sgdh.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.sgdh.R
import com.example.sgdh.databinding.ActivityLoginBinding
import com.example.sgdh.ui.main.MainActivity
import com.example.sgdh.data.api.RetrofitClient
import com.example.sgdh.data.api.LoginRequest
import com.example.sgdh.data.api.GoogleLoginRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pantalla de Inicio de Sesión (LoginActivity).
 * Maneja la autenticación mediante:
 * 1. Correo y Contraseña (Laravel Sanctum).
 * 2. Google Sign-In (OAuth integrado con Laravel).
 * 3. Biometría (Huella digital) para sesiones persistentes.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var biometricPrompt: BiometricPrompt

    /**
     * Launcher para manejar el resultado de la actividad de Google Sign-In.
     * Si es exitoso, extrae el ID Token y lo envía al backend para validación.
     */
    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    performGoogleLogin(idToken)
                } else {
                    showToast("Error: Google no devolvió un token válido.")
                }
            } catch (e: ApiException) {
                Log.e("LOGIN_ERROR", "Fallo conexión con Google: ${e.statusCode}", e)
                showToast("No se pudo conectar con Google.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar hardware biométrico
        setupBiometricLogic()

        // Determinar qué interfaz mostrar (Login o Huella)
        checkExistingSession()

        // Configurar listeners de botones
        setupButtons()
    }

    /**
     * Verifica si existen credenciales guardadas en SharedPreferences.
     * - Si existen: Muestra la interfaz de Biometría.
     * - Si no: Muestra el formulario de Login tradicional.
     */
    private fun checkExistingSession() {
        val prefs = getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)
        val nombreUsuario = prefs.getString("user_name", "Usuario")

        if (token != null) {
            // Modo "Re-ingreso": Ocultar formulario, mostrar huella
            binding.layoutLoginNormal.visibility = View.GONE
            binding.layoutBiometrico.visibility = View.VISIBLE
            binding.tvWelcomeBack.text = getString(R.string.biometric_title) + " $nombreUsuario" // O usa string format

            // Intentar lanzar el prompt biométrico automáticamente
            lanzarHuella()
        } else {
            // Modo "Login Nuevo": Mostrar formulario
            binding.layoutLoginNormal.visibility = View.VISIBLE
            binding.layoutBiometrico.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        // 1. Login Tradicional
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                performLogin(email, password)
            } else {
                showToast("Por favor ingresa correo y contraseña")
            }
        }

        // 2. Login con Google
        binding.btnGoogle.setOnClickListener {
            initiateGoogleLogin()
        }

        // 3. Reintentar Huella (Botón del candado)
        binding.btnFingerprintAction.setOnClickListener {
            lanzarHuella()
        }

        // 4. Cambiar de cuenta (Borrar sesión local)
        binding.btnSwitchAccount.setOnClickListener {
            getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            binding.layoutBiometrico.visibility = View.GONE
            binding.layoutLoginNormal.visibility = View.VISIBLE
        }
    }

    /**
     * Configura e inicia el cliente de Google Sign-In.
     * Cierra sesión previa para permitir seleccionar cuenta nuevamente.
     */
    private fun initiateGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id))
            .requestEmail()
            .build()

        val googleClient = GoogleSignIn.getClient(this, gso)
        googleClient.signOut().addOnCompleteListener {
            googleLauncher.launch(googleClient.signInIntent)
        }
    }

    // --- LÓGICA DE API (RETROFIT) ---

    /**
     * Realiza la petición de Login tradicional al backend (Laravel).
     * @param email Correo del usuario.
     * @param pass Contraseña en texto plano.
     */
    private fun performLogin(email: String, pass: String) {
        // Feedback visual: Deshabilitar botón
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Conectando..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.login(LoginRequest(email, pass))

                withContext(Dispatchers.Main) {
                    // Restaurar botón
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = getString(R.string.login)

                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        saveSessionAndNavigate(data.token, data.user.name, data.user.email)
                    } else {
                        // Manejo de errores de negocio (401, 422)
                        // Aquí podrías parsear el JSON de error para ser más específico
                        showToast("Credenciales incorrectas o usuario no autorizado.")
                        Log.w("LOGIN_API", "Error: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = getString(R.string.login)
                    Log.e("LOGIN_API", "Error de red", e)
                    showToast("No se pudo conectar con el servidor. Verifica tu internet.")
                }
            }
        }
    }

    /**
     * Envía el token de Google al backend para intercambiarlo por un token de sesión propio.
     * @param googleToken ID Token obtenido de Google.
     */
    private fun performGoogleLogin(googleToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.loginWithGoogle(GoogleLoginRequest(googleToken))

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        saveSessionAndNavigate(data.token, data.user.name, data.user.email)
                    } else {
                        showToast("Tu cuenta de Google no está registrada en el sistema.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error de conexión al validar con Google.")
                }
            }
        }
    }

    /**
     * Guarda el token en almacenamiento local seguro y navega al Dashboard.
     */
    private fun saveSessionAndNavigate(token: String, name: String, email: String) {
        getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE).edit().apply {
            putString("token", token)
            putString("user_name", name)
            putString("user_email", email)
            apply()
        }

        // Único Toast de éxito permitido
        showToast("¡Bienvenido, $name!")
        irAlMenuPrincipal()
    }

    // --- BIOMETRÍA ---

    /**
     * Inicializa el callback de Biometría.
     * Define qué hacer si la huella es correcta o incorrecta.
     */
    private fun setupBiometricLogic() {
        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Acceso concedido
                    irAlMenuPrincipal()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Ignorar cancelación por usuario o botón negativo para no spammear
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        showToast("Error Biometría: $errString")
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    showToast("Huella no reconocida. Intenta de nuevo.")
                }
            })
    }

    /**
     * Muestra el diálogo del sistema para solicitar la huella.
     */
    private fun lanzarHuella() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso SGDH")
            .setSubtitle("Confirma tu identidad para continuar")
            .setNegativeButtonText("Usar contraseña")
            .build()
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e("BIOMETRIC", "No se pudo iniciar el hardware biométrico", e)
        }
    }

    private fun irAlMenuPrincipal() {
        startActivity(Intent(this, MainActivity::class.java))
        finish() // Finaliza LoginActivity para que no se pueda volver atrás con "Back"
    }

    // Función auxiliar para mostrar mensajes cortos
    private fun showToast(mensaje: String) {
        Toast.makeText(applicationContext, mensaje, Toast.LENGTH_SHORT).show()
    }
}