package com.example.sgdh.ui.bitacoras

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.sgdh.R
import com.example.sgdh.data.local.AppDatabase
import com.example.sgdh.data.local.BitacoraEntity
import com.example.sgdh.databinding.FragmentBitacorasBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BitacorasFragment : Fragment() {

    private var _binding: FragmentBitacorasBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: BitacorasAdapter

    // Variables para Foto
    private var currentPhotoPath: String? = null
    private var imgPreview: ImageView? = null

    // 1. Lanzador para PEDIR PERMISO de Cámara
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            tomarFoto() // Si el usuario dice "Sí", abrimos la cámara
        } else {
            Toast.makeText(context, "Se necesita permiso para la foto", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Lanzador para OBTENER LA FOTO (El resultado de la cámara)
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoPath != null) {
            // Cargar la foto en la vista previa del diálogo
            imgPreview?.load(File(currentPhotoPath!!)) {
                crossfade(true)
            }
        } else {
            currentPhotoPath = null // Si canceló, limpiamos la ruta
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBitacorasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvBitacoras.layoutManager = LinearLayoutManager(context)

        // Configurar Adapter con acciones CRUD
        adapter = BitacorasAdapter(
            lista = emptyList(),
            onItemClick = { bitacora -> verDetalle(bitacora) },
            onEditClick = { bitacora -> editarBitacora(bitacora) },
            onDeleteClick = { bitacora -> confirmarEliminacion(bitacora) }
        )
        binding.rvBitacoras.adapter = adapter

        cargarDatos()

        // Botón "+"
        binding.fabAdd.setOnClickListener {
            mostrarDialogoFormulario(null) // null = Nueva Bitácora
        }
    }

    private fun cargarDatos() {
        val prefs = requireActivity().getSharedPreferences("sgdh_prefs", android.content.Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1) // Obtenemos el ID del usuario actual

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())

            // Usamos la nueva función filtrada
            val lista = db.bitacoraDao().obtenerPorUsuario(userId)

            withContext(Dispatchers.Main) {
                adapter.updateData(lista)
                binding.tvEmpty.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    // --- LÓGICA DEL FORMULARIO (Crear/Editar) ---

    private fun editarBitacora(bitacora: BitacoraEntity) {
        mostrarDialogoFormulario(bitacora)
    }

    private fun mostrarDialogoFormulario(bitacoraAEditar: BitacoraEntity?) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_bitacora, null)
        val etTitulo = dialogView.findViewById<EditText>(R.id.etTitulo)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDesc)
        val btnFoto = dialogView.findViewById<MaterialButton>(R.id.btnFoto)
        imgPreview = dialogView.findViewById(R.id.imgPreview)

        currentPhotoPath = null
        val esEdicion = bitacoraAEditar != null

        // Si es edición, rellenamos datos
        if (esEdicion) {
            etTitulo.setText(bitacoraAEditar!!.titulo)
            etDesc.setText(bitacoraAEditar.descripcion)
            if (bitacoraAEditar.fotoPath != null) {
                currentPhotoPath = bitacoraAEditar.fotoPath
                imgPreview?.load(File(currentPhotoPath!!))
            }
        }

        // Clic en "Tomar Foto" -> Verifica permisos primero
        btnFoto.setOnClickListener {
            verificarPermisosYTomarFoto()
        }

        val tituloDialogo = if (esEdicion) "Editar Bitácora" else "Nueva Bitácora"

        AlertDialog.Builder(requireContext())
            .setTitle(tituloDialogo)
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                if (etTitulo.text.isNotEmpty()) {
                    guardarEnBD(
                        titulo = etTitulo.text.toString(),
                        desc = etDesc.text.toString(),
                        bitacoraOriginal = bitacoraAEditar
                    )
                } else {
                    Toast.makeText(context, "El título es obligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- LÓGICA DE CÁMARA SEGURA ---

    private fun verificarPermisosYTomarFoto() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido -> Abrir cámara
                tomarFoto()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Explicar al usuario por qué necesitamos la cámara (Opcional, aquí pedimos directo)
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                // Pedir permiso por primera vez
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun tomarFoto() {
        try {
            val file = crearArchivoImagen()
            // IMPORTANTE: Usamos la autoridad exacta que pusimos en el Manifest
            val authority = "com.example.sgdh.provider"

            val uri = FileProvider.getUriForFile(requireContext(), authority, file)
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al iniciar cámara: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun crearArchivoImagen(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        // USAMOS CACHÉ para evitar problemas de permisos en Android 11+
        val dir = requireContext().externalCacheDir
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", dir).apply {
            currentPhotoPath = absolutePath
        }
    }

    // --- LÓGICA DE BASE DE DATOS ---

    private fun guardarEnBD(titulo: String, desc: String, bitacoraOriginal: BitacoraEntity?) {
        val fechaActual = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val db = AppDatabase.getDatabase(requireContext())

        // Obtenemos el ID del usuario para marcar la bitácora
        val prefs = requireActivity().getSharedPreferences("sgdh_prefs", android.content.Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(context, "Error de sesión", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            if (bitacoraOriginal == null) {
                // INSERTAR NUEVO (Con usuarioId)
                val nueva = BitacoraEntity(
                    titulo = titulo, descripcion = desc, fecha = fechaActual,
                    fotoPath = currentPhotoPath,
                    usuarioId = userId // <--- ASIGNAMOS DUEÑO
                )
                db.bitacoraDao().insertar(nueva)
            } else {
                // ACTUALIZAR EXISTENTE
                val editada = bitacoraOriginal.copy(
                    titulo = titulo,
                    descripcion = desc,
                    fotoPath = currentPhotoPath ?: bitacoraOriginal.fotoPath
                    // usuarioId ya viene en el original, no cambia
                )
                db.bitacoraDao().actualizar(editada)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Guardado correctamente", Toast.LENGTH_SHORT).show()
                cargarDatos()
            }
        }
    }

    private fun confirmarEliminacion(bitacora: BitacoraEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar")
            .setMessage("¿Borrar '${bitacora.titulo}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarEnBD(bitacora)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarEnBD(bitacora: BitacoraEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getDatabase(requireContext()).bitacoraDao().eliminar(bitacora)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Eliminado", Toast.LENGTH_SHORT).show()
                cargarDatos()
            }
        }
    }

    private fun verDetalle(bitacora: BitacoraEntity) {
        val intent = Intent(context, DetalleBitacoraActivity::class.java)
        intent.putExtra("EXTRA_BITACORA_ID", bitacora.id)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}