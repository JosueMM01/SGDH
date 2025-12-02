package com.example.sgdh.data.api

import android.content.Context
import android.content.Intent
import com.example.sgdh.ui.login.LoginActivity
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Si el servidor dice 401 (No autorizado) es que el token venció
        if (response.code == 401) {
            // 1. Borrar datos locales
            val prefs = context.getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()

            // 2. Redirigir al Login forzosamente
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }

        return response
    }
}