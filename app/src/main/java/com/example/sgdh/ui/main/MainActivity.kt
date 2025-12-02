package com.example.sgdh.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.sgdh.R
import com.example.sgdh.ui.bitacoras.BitacorasFragment // <--- IMPORTANTE
import com.example.sgdh.ui.home.HomeFragment
import com.example.sgdh.ui.login.LoginActivity
import com.example.sgdh.ui.solicitudes.SolicitudesFragment
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbar)

        // Configurar botón hamburguesa
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        setupSidebarUserInfo()
        setupMenuActions()

        // --- CARGA INICIAL ---
        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), "Inicio") // Carga el Home por defecto
        }
    }

    private fun setupMenuActions() {
        // 1. Botón Inicio
        findViewById<View>(R.id.menu_inicio)?.setOnClickListener {
            loadFragment(HomeFragment(), "Inicio")
        }

        // 2. Botón Solicitudes
        findViewById<View>(R.id.menu_solicitudes)?.setOnClickListener {
            loadFragment(SolicitudesFragment(), "Mis Solicitudes")
        }

        // 3. Botón Bitácora (NUEVO - Conectado)
        findViewById<View>(R.id.menu_bitacora)?.setOnClickListener {
            loadFragment(BitacorasFragment(), "Bitácoras")
        }

        // 4. Botón Salir
        findViewById<View>(R.id.btnLogout)?.setOnClickListener {
            logout()
        }
    }

    private fun loadFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()

        toolbar.title = title // Cambia el título de la barra superior
        drawerLayout.closeDrawer(GravityCompat.START) // Cierra el menú automáticamente
    }

    private fun setupSidebarUserInfo() {
        val prefs = getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("user_name", "Usuario")
        val userEmail = prefs.getString("user_email", "correo@sgdh.mx")

        // Buscamos las vistas del menú lateral
        val tvName = findViewById<TextView>(R.id.tvUserName)
        val tvEmail = findViewById<TextView>(R.id.tvUserEmail)

        // Asignamos los datos si las vistas existen
        if (tvName != null) tvName.text = userName
        if (tvEmail != null) tvEmail.text = userEmail
    }

    private fun logout() {
        // Borrar sesión
        getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE).edit().clear().apply()

        // Ir al Login y borrar historial
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}