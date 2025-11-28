package com.example.sgdh.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.ImageView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.sgdh.R
import com.example.sgdh.data.api.RetrofitClient
import com.example.sgdh.ui.login.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        // Configurar botón hamburguesa
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Cargar datos del usuario en el Sidebar (Footer)
        setupSidebarUserData()

        // Configurar Clicks del Menú
        setupMenuActions()
    }

    private fun setupSidebarUserData() {
        val prefs = getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("user_name", "Usuario")
        val userEmail = prefs.getString("user_email", "correo@sgdh.mx")

        // Referencias directas a los IDs del include
        findViewById<TextView>(R.id.tvUserName).text = userName
        findViewById<TextView>(R.id.tvUserEmail).text = userEmail
    }

    private fun setupMenuActions() {
        // Helper para cerrar drawer y cambiar título
        fun navigateTo(title: String) {
            findViewById<MaterialToolbar>(R.id.toolbar).title = title
            drawerLayout.closeDrawer(GravityCompat.START)
            // Aquí agregarías la lógica de reemplazar Fragments
            // supportFragmentManager.beginTransaction().replace(...).commit()
        }

        // Click Listeners del Menú Custom
        findViewById<View>(R.id.menu_inicio).setOnClickListener { navigateTo("Inicio") }
        findViewById<View>(R.id.menu_solicitudes).setOnClickListener { navigateTo("Mis Solicitudes") }
        findViewById<View>(R.id.menu_dotacion).setOnClickListener { navigateTo("Dotación") }

        // Lógica de Logout
        findViewById<View>(R.id.btnLogout).setOnClickListener {
            // Borrar Token
            getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            // Ir al Login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}