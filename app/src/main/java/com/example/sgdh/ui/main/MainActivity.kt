package com.example.sgdh.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.sgdh.R
import com.example.sgdh.ui.bitacoras.BitacorasFragment
import com.example.sgdh.ui.home.HomeFragment
import com.example.sgdh.ui.login.LoginActivity
import com.example.sgdh.ui.solicitudes.SolicitudesFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pedirPermisoNotificaciones()

        drawerLayout = findViewById(R.id.drawer_layout)
        toolbar = findViewById(R.id.toolbar)

        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        setupSidebarUserInfo()
        setupMenuActions()

        if (savedInstanceState == null) {
            loadFragment(HomeFragment(), "Inicio")
        }
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun setupMenuActions() {
        findViewById<View>(R.id.menu_inicio)?.setOnClickListener {
            loadFragment(HomeFragment(), "Inicio")
        }

        findViewById<View>(R.id.menu_solicitudes)?.setOnClickListener {
            loadFragment(SolicitudesFragment(), "Mis Solicitudes")
        }

        findViewById<View>(R.id.menu_bitacora)?.setOnClickListener {
            loadFragment(BitacorasFragment(), "Bitácoras")
        }

        findViewById<View>(R.id.btnLogout)?.setOnClickListener {
            logout()
        }
    }

    private fun loadFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment_content_main, fragment)
            .commit()

        toolbar.title = title
        drawerLayout.closeDrawer(GravityCompat.START)
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
        val prefs = getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        // CORRECCIÓN: Usar 'sgdh_user_' para desuscribirse correctamente
        if (userId != -1) {
            val topicName = "sgdh_user_$userId"
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topicName)
        }

        prefs.edit().clear().apply()

        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}