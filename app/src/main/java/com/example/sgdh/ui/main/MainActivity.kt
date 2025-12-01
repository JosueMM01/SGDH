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
import com.example.sgdh.ui.login.LoginActivity
import com.example.sgdh.ui.home.HomeFragment
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

        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        setupSidebarUserInfo()
        setupMenuActions()

        // --- CARGA INICIAL ---
        // Si es la primera vez que se abre (no es una rotación de pantalla)
        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), "Inicio") // Carga el Home por defecto
        }
    }

    private fun setupMenuActions() {
        // Botón Inicio -> Carga HomeFragment
        findViewById<View>(R.id.menu_inicio)?.setOnClickListener {
            loadFragment(HomeFragment(), "Inicio")
        }

        // Botón Solicitudes -> Carga SolicitudesFragment
        findViewById<View>(R.id.menu_solicitudes)?.setOnClickListener {
            loadFragment(SolicitudesFragment(), "Mis Solicitudes")
        }

        // Botón Salir
        findViewById<View>(R.id.btnLogout)?.setOnClickListener {
            logout()
        }

        // (Otros botones como Dotación pueden quedar pendientes o mostrar un Toast)
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

        val tvName = findViewById<TextView>(R.id.tvUserName)
        val tvEmail = findViewById<TextView>(R.id.tvUserEmail)

        if (tvName != null) tvName.text = userName
        if (tvEmail != null) tvEmail.text = userEmail
    }

    private fun logout() {
        getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}