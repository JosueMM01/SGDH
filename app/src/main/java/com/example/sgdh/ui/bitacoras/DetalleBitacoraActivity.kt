package com.example.sgdh.ui.bitacoras

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.sgdh.data.local.AppDatabase
import com.example.sgdh.databinding.ActivityDetalleBitacoraBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DetalleBitacoraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetalleBitacoraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetalleBitacoraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getIntExtra("EXTRA_BITACORA_ID", -1)
        if (id != -1) cargarDetalle(id) else finish()
    }

    private fun cargarDetalle(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val item = AppDatabase.getDatabase(this@DetalleBitacoraActivity).bitacoraDao().obtenerPorId(id)
            withContext(Dispatchers.Main) {
                if (item != null) {
                    binding.tvTituloDetalle.text = item.titulo
                    binding.tvFechaDetalle.text = item.fecha
                    binding.tvDescDetalle.text = item.descripcion
                    if (item.fotoPath != null) {
                        binding.imgDetalle.load(File(item.fotoPath))
                    }
                }
            }
        }
    }
}