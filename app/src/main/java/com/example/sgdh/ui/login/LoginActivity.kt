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
import com.example.sgdh.data.api.GoogleLoginRequest
import com.example.sgdh.data.api.LoginRequest
import com.example.sgdh.data.api.RetrofitClient
import com.example.sgdh.data.api.UserDto
import com.example.sgdh.databinding.ActivityLoginBinding
import com.example.sgdh.ui.main.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var biometricPrompt: BiometricPrompt

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

        setupBiometricLogic()
        checkExistingSession()
        setupButtons()
    }

    private fun checkExistingSession() {
        val prefs = getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)
        val nombreUsuario = prefs.getString("user_name", "Usuario")

        if (token != null) {
            binding.layoutLoginNormal.visibility = View.GONE
            binding.layoutBiometrico.visibility = View.VISIBLE
            binding.tvWelcomeBack.text = "${getString(R.string.biometric_title)} $nombreUsuario"
            lanzarHuella()
        } else {
            binding.layoutLoginNormal.visibility = View.VISIBLE
            binding.layoutBiometrico.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                performLogin(email, password)
            } else {
                showToast("Por favor ingresa correo y contraseña")
            }
        }

        binding.btnGoogle.setOnClickListener {
            initiateGoogleLogin()
        }

        binding.btnFingerprintAction.setOnClickListener {
            lanzarHuella()
        }

        binding.btnSwitchAccount.setOnClickListener {
            getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            binding.layoutBiometrico.visibility = View.GONE
            binding.layoutLoginNormal.visibility = View.VISIBLE
        }
    }

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

    private fun performLogin(email: String, pass: String) {
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Conectando..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.getApi(this@LoginActivity).login(LoginRequest(email, pass))

                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = getString(R.string.login)

                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        onLoginSuccess(data.token, data.user)
                    } else {
                        showToast("Credenciales incorrectas o usuario no autorizado.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = getString(R.string.login)
                    showToast("No se pudo conectar con el servidor.")
                }
            }
        }
    }

    private fun performGoogleLogin(googleToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.getApi(this@LoginActivity).loginWithGoogle(GoogleLoginRequest(googleToken))

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        onLoginSuccess(data.token, data.user)
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

    private fun onLoginSuccess(token: String, user: UserDto) {
        val prefs = getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("token", token)
            putInt("user_id", user.id)
            putString("user_name", user.name)
            putString("user_email", user.email)
            putString("user_role", user.rol)
            putInt("user_area_id", user.areaId ?: -1)
            apply()
        }

        showToast("¡Bienvenido, ${user.name}!")

        // CORRECCIÓN: Usar 'sgdh_user_' para coincidir con Laravel
        val topicName = "sgdh_user_${user.id}"
        FirebaseMessaging.getInstance().subscribeToTopic(topicName)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("FCM", "Error al suscribirse al tema $topicName")
                } else {
                    Log.d("FCM", "Suscrito al tema $topicName")
                }
            }

        irAlMenuPrincipal()
    }

    private fun irAlMenuPrincipal() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupBiometricLogic() {
        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    val prefs = getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
                    val userId = prefs.getInt("user_id", -1)
                    if (userId != -1) {
                        // CORRECCIÓN AQUÍ TAMBIÉN
                        FirebaseMessaging.getInstance().subscribeToTopic("sgdh_user_$userId")
                    }

                    irAlMenuPrincipal()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        showToast("Error Biometría: $errString")
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    showToast("Huella no reconocida.")
                }
            })
    }

    private fun lanzarHuella() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso SGDH")
            .setSubtitle("Confirma tu identidad")
            .setNegativeButtonText("Usar contraseña")
            .build()
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e("BIOMETRIC", "No se pudo iniciar hardware", e)
        }
    }

    private fun showToast(mensaje: String) {
        Toast.makeText(applicationContext, mensaje, Toast.LENGTH_SHORT).show()
    }
}