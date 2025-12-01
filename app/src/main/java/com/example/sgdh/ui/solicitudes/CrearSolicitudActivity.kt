package com.example.sgdh.ui.solicitudes

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sgdh.data.api.CreateSolicitudRequest
import com.example.sgdh.data.api.DetalleSolicitudDto
import com.example.sgdh.data.api.RetrofitClient
import com.example.sgdh.databinding.ActivityCrearSolicitudBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CrearSolicitudActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrearSolicitudBinding
    private lateinit var adapter: ProductosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrearSolicitudBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Toolbar
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Configurar Lista
        binding.rvProductos.layoutManager = LinearLayoutManager(this)
        adapter = ProductosAdapter(emptyList()) {
            actualizarBotonEnvio()
        }
        binding.rvProductos.adapter = adapter

        // Botón Enviar
        binding.btnEnviar.setOnClickListener {
            enviarSolicitud()
        }

        // Cargar Catálogo
        cargarProductos()
    }

    private fun cargarProductos() {
        val prefs = getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Usamos getApi(this) para tener seguridad con Interceptor
                val response = RetrofitClient.getApi(this@CrearSolicitudActivity).getProductos("Bearer $token")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        adapter.updateData(response.body()!!.data)
                    } else {
                        Toast.makeText(this@CrearSolicitudActivity, "Error cargando productos", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CrearSolicitudActivity, "Error de red", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun actualizarBotonEnvio() {
        val totalItems = adapter.selecciones.values.sum()
        if (totalItems > 0) {
            binding.btnEnviar.visibility = View.VISIBLE
            binding.btnEnviar.text = "Enviar Solicitud ($totalItems)"
        } else {
            binding.btnEnviar.visibility = View.GONE
        }
    }

    private fun enviarSolicitud() {
        val justificacion = binding.etJustificacion.text.toString().trim()
        if (justificacion.isEmpty()) {
            binding.etJustificacion.error = "Escribe una justificación"
            return
        }

        val prefs = getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""

        // Convertir el mapa de selecciones a la lista que pide la API
        val detalles = adapter.selecciones.filter { it.value > 0 }.map { (id, cantidad) ->
            DetalleSolicitudDto(productoId = id, cantidad = cantidad)
        }

        val request = CreateSolicitudRequest(justificacion, detalles)

        binding.btnEnviar.isEnabled = false
        binding.btnEnviar.text = "Enviando..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.getApi(this@CrearSolicitudActivity).createSolicitud("Bearer $token", request)

                withContext(Dispatchers.Main) {
                    binding.btnEnviar.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(this@CrearSolicitudActivity, "¡Solicitud Creada!", Toast.LENGTH_LONG).show()
                        finish() // Cierra la pantalla y vuelve a la lista
                    } else {
                        val error = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(this@CrearSolicitudActivity, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnEnviar.isEnabled = true
                    Toast.makeText(this@CrearSolicitudActivity, "Falló el envío", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}