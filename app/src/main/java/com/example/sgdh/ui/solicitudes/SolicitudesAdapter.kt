package com.example.sgdh.ui.solicitudes

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sgdh.data.api.Solicitud
import com.example.sgdh.databinding.ItemSolicitudBinding

class SolicitudesAdapter(
    private var lista: List<Solicitud>,
    private val onItemClick: (Int) -> Unit // Callback para manejar el clic
) : RecyclerView.Adapter<SolicitudesAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSolicitudBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSolicitudBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        holder.binding.apply {
            // Datos
            tvFolio.text = "SOL-#${item.id}"

            // Fecha (Cortar hora si existe)
            tvFecha.text = if (item.fecha.length >= 10) item.fecha.substring(0, 10) else item.fecha

            tvArea.text = item.area?.nombre ?: "Sin Área"
            tvTotal.text = item.justificacion ?: "Sin justificación"

            // Chip de Estatus
            chipEstatus.text = item.estatus.label

            val color = when(item.estatus.value) {
                "aprobada" -> Color.parseColor("#22c55e") // Verde
                "rechazada" -> Color.parseColor("#ef4444") // Rojo
                "surtida" -> Color.parseColor("#3b82f6")   // Azul
                "pendiente_jefe", "pendiente_farmacia" -> Color.parseColor("#f59e0b") // Naranja
                else -> Color.parseColor("#64748b")        // Gris
            }
            chipEstatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color))

            // CLIC EN LA TARJETA -> Ir al Detalle
            holder.itemView.setOnClickListener {
                onItemClick(item.id)
            }
        }
    }

    override fun getItemCount() = lista.size

    fun updateData(nuevaLista: List<Solicitud>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}