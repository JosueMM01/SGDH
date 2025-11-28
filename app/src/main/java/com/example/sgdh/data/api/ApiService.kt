package com.example.sgdh.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import com.google.gson.annotations.SerializedName

// --- MODELOS DE DATOS ---

data class LoginRequest(
    val email: String,
    val password: String,
    @SerializedName("device_name") val deviceName: String = "AndroidApp"
)

data class GoogleLoginRequest(
    @SerializedName("id_token") val token: String,
    @SerializedName("device_name") val deviceName: String = "AndroidApp"
)

data class LoginResponse(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    @SerializedName("area_id") val areaId: Int?
)

// --- INTERFAZ DE CONEXIÓN ---

interface ApiService {

    @POST("auth/token")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<LoginResponse>
}