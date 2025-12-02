package com.example.sgdh.ui.bitacoras

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.sgdh.data.local.BitacoraEntity
import com.example.sgdh.databinding.ItemBitacoraBinding

class BitacorasAdapter(
    private var lista: List<BitacoraEntity>,
    private val onItemClick: (BitacoraEntity) -> Unit,
    private val onEditClick: (BitacoraEntity) -> Unit,   // Acción Editar
    private val onDeleteClick: (BitacoraEntity) -> Unit  // Acción Eliminar
) : RecyclerView.Adapter<BitacorasAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBitacoraBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBitacoraBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.binding.apply {
            tvTitulo.text = item.titulo
            tvDescripcion.text = item.descripcion
            // Cortar fecha para que se vea bien
            tvFecha.text = if (item.fecha.length >= 10) item.fecha.substring(0, 10) else item.fecha

            // 1. Clic en toda la tarjeta -> Ver Detalle
            root.setOnClickListener { onItemClick(item) }

            // 2. Clic en los 3 puntos -> Menú Popup
            btnMenu.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add("Editar")
                popup.menu.add("Eliminar")

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.title) {
                        "Editar" -> onEditClick(item)
                        "Eliminar" -> onDeleteClick(item)
                    }
                    true
                }
                popup.show()
            }
        }
    }

    override fun getItemCount() = lista.size

    fun updateData(nuevaLista: List<BitacoraEntity>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}