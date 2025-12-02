package com.example.sgdh.ui.solicitudes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.sgdh.R
import com.example.sgdh.data.api.DetalleProductoDto
import com.example.sgdh.databinding.ItemProductoCarritoBinding
// Reusamos el layout del carrito pero escondemos los botones

class DetalleProductoAdapter(private var lista: List<DetalleProductoDto>) :
    RecyclerView.Adapter<DetalleProductoAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemProductoCarritoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductoCarritoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        holder.binding.apply {
            tvNombre.text = item.producto.descripcion
            tvPresentacion.text = item.producto.presentacion

            // Reusamos el diseño, pero ocultamos los controles de edición
            btnAdd.visibility = android.view.View.GONE
            btnRemove.visibility = android.view.View.GONE
            tvStock.text = "Cantidad solicitada"

            tvCantidad.text = item.cantidad.toString()
            tvCantidad.setTextColor(holder.itemView.context.getColor(R.color.brand_primary))

            imgProducto.load(item.producto.imageUrl) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
            }
        }
    }

    override fun getItemCount() = lista.size
}