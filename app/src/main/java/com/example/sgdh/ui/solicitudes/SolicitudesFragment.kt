package com.example.sgdh.ui.solicitudes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sgdh.data.api.RetrofitClient
import com.example.sgdh.databinding.FragmentSolicitudesBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SolicitudesFragment : Fragment() {

    private var _binding: FragmentSolicitudesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SolicitudesAdapter

    // Variables de sesión
    private var userRole: String = ""
    private var userAreaId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSolicitudesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Obtener datos de sesión
        val prefs = requireActivity().getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
        userRole = prefs.getString("user_role", "") ?: ""
        userAreaId = prefs.getInt("user_area_id", -1)
        val token = prefs.getString("token", "") ?: ""

        // 2. Configurar UI según Rol (Mostrar/Ocultar FAB)
        configurarVistaPorRol()

        // 3. Configurar Lista y Click Listener
        binding.rvSolicitudes.layoutManager = LinearLayoutManager(context)

        adapter = SolicitudesAdapter(emptyList()) { idSolicitud ->
            // Al hacer clic en una solicitud, abrimos el Detalle
            val intent = Intent(context, DetalleSolicitudActivity::class.java)
            intent.putExtra("EXTRA_SOLICITUD_ID", idSolicitud)
            startActivity(intent)
        }

        binding.rvSolicitudes.adapter = adapter

        // 4. Cargar datos iniciales
        if (token.isNotEmpty() && userAreaId != -1) {
            cargarSolicitudes(token)
        }
    }

    // Recargar lista al volver (por si creaste una nueva o aprobaste una)
    override fun onResume() {
        super.onResume()
        val prefs = requireActivity().getSharedPreferences("sgdh_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        if (token.isNotEmpty() && userAreaId != -1) {
            cargarSolicitudes(token)
        }
    }

    private fun configurarVistaPorRol() {
        // Si no tiene área -> Error
        if (userAreaId == -1) {
            binding.rvSolicitudes.visibility = View.GONE
            binding.fabCreate.visibility = View.GONE
            binding.layoutNoArea.visibility = View.VISIBLE
            return
        }

        // Si es Personal -> Botón de Crear (Carrito)
        if (userRole == "personal_area") {
            binding.fabCreate.visibility = View.VISIBLE
            binding.fabCreate.setOnClickListener {
                val intent = Intent(context, CrearSolicitudActivity::class.java)
                startActivity(intent)
            }
        } else {
            // Jefe de Área -> Solo ve la lista
            binding.fabCreate.visibility = View.GONE
        }
    }

    private fun cargarSolicitudes(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Usamos requireContext() para la seguridad del token
                val response = RetrofitClient.getApi(requireContext()).getSolicitudes("Bearer $token")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val lista = response.body()!!.data
                        if (lista.isEmpty()) {
                            // Opcional: Mostrar mensaje de lista vacía si quieres
                        }
                        adapter.updateData(lista)
                    } else {
                        // Si falla silenciosamente o muestras toast
                        // Toast.makeText(context, "Error cargando lista", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}