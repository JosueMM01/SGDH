package com.example.sgdh.data.api

import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import com.google.gson.annotations.SerializedName

// ==========================================
// 1. INTERFAZ DE CONEXIÓN (ENDPOINTS)
// ==========================================
interface ApiService {

    // --- AUTENTICACIÓN ---
    @POST("auth/token")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<LoginResponse>

    // --- SOLICITUDES (Lectura) ---
    @GET("solicitudes")
    suspend fun getSolicitudes(
        @Header("Authorization") token: String
    ): Response<SolicitudesResponse>

    // --- SOLICITUDES (Escritura / Carrito) ---
    @POST("solicitudes")
    suspend fun createSolicitud(
        @Header("Authorization") token: String,
        @Body request: CreateSolicitudRequest
    ): Response<SolicitudResponseSingle>

    // --- PRODUCTOS (Catálogo para el carrito) ---
    @GET("productos")
    suspend fun getProductos(
        @Header("Authorization") token: String
    ): Response<ProductosResponse>

    // Obtener UNA solicitud con sus productos
    @GET("solicitudes/{id}")
    suspend fun getSolicitudDetalle(
        @Header("Authorization") token: String,
        @retrofit2.http.Path("id") id: Int
    ): Response<SolicitudDetalleResponse>

    // Aprobar/Rechazar (El Jefe envía un status nuevo)
    // CAMBIO: Quitamos "/estado" del final de la ruta
    @retrofit2.http.PATCH("solicitudes/{id}/estatus")
    suspend fun updateEstadoSolicitud(
        @Header("Authorization") token: String,
        @retrofit2.http.Path("id") id: Int,
        @Body body: UpdateEstadoRequest
    ): Response<Any>

    // --- NOTIFICACIONES ---
    @POST("user/fcm-token")
    suspend fun updateFcmToken(
        @Header("Authorization") token: String,
        @Body body: FcmTokenRequest
    ): Response<Any>

    @DELETE("user/fcm-token")
    suspend fun deleteFcmToken(
        @Header("Authorization") token: String
    ): Response<Any>

}

// ==========================================
// 2. MODELOS DE DATOS (DTOs)
// ==========================================

// --- LOGIN ---
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
    @SerializedName("rol") val rol: String?,
    @SerializedName("area_id") val areaId: Int?
)

// --- LISTA DE SOLICITUDES (Historial) ---
data class SolicitudesResponse(
    val data: List<Solicitud>
)

data class Solicitud(
    val id: Int,
    val estatus: EstatusDto,
    val justificacion: String?,
    @SerializedName("fecha_solicitud") val fecha: String,
    @SerializedName("total_productos") val totalProductos: Int?,
    val area: AreaDto?,
    @SerializedName("usuario_solicitante") val usuario: UsuarioDto?
)

data class EstatusDto(val value: String, val label: String)
data class AreaDto(val id: Int, val nombre: String)
data class UsuarioDto(val id: Int, val nombre: String)

// --- CATÁLOGO DE PRODUCTOS (Para el Carrito) ---
data class ProductosResponse(
    val data: List<DotacionProducto>
)

data class DotacionProducto(
    val producto: ProductoDetail,
    val dotacion: DotacionDetail
)

data class ProductoDetail(
    val id: Int,
    val clave: String,
    val descripcion: String,
    val presentacion: String,
    @SerializedName("image_url") val imageUrl: String?, // <--- ¡AQUÍ LLEGA LA FOTO DEL PRODUCTO!
    @SerializedName("stock_disponible") val stock: Int
)

data class DotacionDetail(
    @SerializedName("cantidad_diaria") val cantidadDiaria: Int
)

// --- CREAR SOLICITUD (Enviar al servidor) ---
data class CreateSolicitudRequest(
    val justificacion: String,
    val detalles: List<DetalleSolicitudDto>
)

data class DetalleSolicitudDto(
    @SerializedName("producto_id") val productoId: Int,
    @SerializedName("cantidad_solicitada") val cantidad: Int
)

// Respuesta al crear una solicitud (A veces el backend devuelve el objeto creado)
data class SolicitudResponseSingle(
    val data: Solicitud
)

// Respuesta del detalle (incluye la lista de productos dentro)
data class SolicitudDetalleResponse(
    val data: SolicitudDetalladaDto
)

data class SolicitudDetalladaDto(
    val id: Int,
    val estatus: EstatusDto,
    val justificacion: String?,
    @SerializedName("fecha_solicitud") val fecha: String,
    val detalles: List<DetalleProductoDto>, // La lista de productos
    @SerializedName("usuario_solicitante") val usuario: UserDto?
)

data class DetalleProductoDto(
    val id: Int,
    @SerializedName("cantidad_solicitada") val cantidad: Int,
    val producto: ProductoDetail // Reusamos el modelo que ya teníamos
)

// Para aprobar/rechazar
data class UpdateEstadoRequest(
    val estatus: String, // "aprobada" o "rechazada"
    @SerializedName("motivo_rechazo") val motivoRechazo: String? = null
)

// Modelo de datos al final
data class FcmTokenRequest(val fcm_token: String)