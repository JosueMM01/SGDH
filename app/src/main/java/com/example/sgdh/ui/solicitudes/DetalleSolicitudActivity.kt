package com.example.sgdh.ui.solicitudes

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sgdh.data.api.RetrofitClient
import com.example.sgdh.data.api.SolicitudDetalladaDto
import com.example.sgdh.data.api.UpdateEstadoRequest
import com.example.sgdh.databinding.ActivityDetalleSolicitudBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetalleSolicitudActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleSolicitudBinding
    private var solicitudId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleSolicitudBinding.inflate(layoutInflater)
        setContentView(binding.root)

        solicitudId = intent.getIntExtra("EXTRA_SOLICITUD_ID", -1)
        if (solicitudId == -1) {
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvDetalles.layoutManager = LinearLayoutManager(this)

        cargarDetalle()
    }

    private fun cargarDetalle() {
        val prefs = getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.getApi(this@DetalleSolicitudActivity)
                    .getSolicitudDetalle("Bearer $token", solicitudId)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        actualizarUI(response.body()!!.data)
                    } else {
                        Toast.makeText(this@DetalleSolicitudActivity, "Error cargando detalle", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DetalleSolicitudActivity, "Error de red", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun actualizarUI(data: SolicitudDetalladaDto) {
        binding.tvFolio.text = "SOL-#${data.id}"
        binding.tvFecha.text = "Fecha: ${if (data.fecha.length >= 10) data.fecha.substring(0, 10) else data.fecha}"
        binding.tvSolicitante.text = "Solicitado por: ${data.usuario?.name ?: "Desconocido"}"
        binding.tvJustificacion.text = data.justificacion ?: "Sin justificación"

        binding.chipEstatus.text = data.estatus.label
        val color = when(data.estatus.value) {
            "aprobada", "surtida" -> Color.parseColor("#22c55e")
            "rechazada" -> Color.parseColor("#ef4444")
            "pendiente_jefe", "pendiente_farmacia" -> Color.parseColor("#f59e0b")
            else -> Color.parseColor("#64748b")
        }
        binding.chipEstatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color))

        binding.rvDetalles.adapter = DetalleProductoAdapter(data.detalles)

        // --- LÓGICA DE JEFE ---
        val prefs = getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
        val role = prefs.getString("user_role", "") ?: ""

        // Solo mostrar si soy JEFE y está PENDIENTE
        if (role == "jefe_area" && data.estatus.value == "pendiente_jefe") {
            binding.layoutAccionesJefe.visibility = View.VISIBLE

            // Acción Única: Dar el Visto Bueno
            binding.btnAprobar.setOnClickListener {
                autorizarSolicitud()
            }
        } else {
            binding.layoutAccionesJefe.visibility = View.GONE
        }
    }

    private fun autorizarSolicitud() {
        val prefs = getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""

        binding.btnAprobar.isEnabled = false
        binding.btnAprobar.text = "Autorizando..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Enviamos directamente el estado que la API espera
                val req = UpdateEstadoRequest(
                    estatus = "pendiente_farmacia", // El único estado permitido para el jefe
                    motivoRechazo = null // No necesario
                )

                val response = RetrofitClient.getApi(this@DetalleSolicitudActivity)
                    .updateEstadoSolicitud("Bearer $token", solicitudId, req)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@DetalleSolicitudActivity, "¡Solicitud Autorizada!", Toast.LENGTH_LONG).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("API_ERROR", "Fallo: $errorBody")
                        Toast.makeText(this@DetalleSolicitudActivity, "Error ${response.code()}: No se pudo autorizar.", Toast.LENGTH_LONG).show()

                        binding.btnAprobar.isEnabled = true
                        binding.btnAprobar.text = "Autorizar Solicitud"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DetalleSolicitudActivity, "Error de red", Toast.LENGTH_SHORT).show()
                    binding.btnAprobar.isEnabled = true
                    binding.btnAprobar.text = "Autorizar Solicitud"
                }
            }
        }
    }
}