package com.example.sgdh.data.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    //  IP del tu servidor
    const val BASE_URL = "http://192.168.1.84/api/"

    // Variable para guardar la instancia única
    @Volatile
    private var apiInstance: ApiService? = null

    // Función para obtener la API. Requiere contexto para configurar la seguridad.
    fun getApi(context: Context): ApiService {
        return apiInstance ?: synchronized(this) {
            val instance = buildRetrofit(context)
            apiInstance = instance
            instance
        }
    }

    private fun buildRetrofit(context: Context): ApiService {
        // 1. Interceptor para ver los logs en la consola (Debug)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 2. Interceptor de Seguridad (Manejo de Token vencido 401)
        val authInterceptor = AuthInterceptor(context)

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor) // <--- Conectamos al guardia aquí
            .addInterceptor { chain ->
                // 3. Forzar siempre el envío y recepción de JSON
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}