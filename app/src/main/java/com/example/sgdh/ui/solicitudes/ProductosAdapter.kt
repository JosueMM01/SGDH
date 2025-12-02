package com.example.sgdh.ui.solicitudes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.sgdh.R
import com.example.sgdh.data.api.DotacionProducto
import com.example.sgdh.databinding.ItemProductoCarritoBinding

class ProductosAdapter(
    private var lista: List<DotacionProducto>,
    private val onCantidadChanged: () -> Unit // Callback para avisar a la Activity que recalculé totales
) : RecyclerView.Adapter<ProductosAdapter.ViewHolder>() {

    // Mapa para guardar cantidades: ID Producto -> Cantidad
    val selecciones = mutableMapOf<Int, Int>()

    class ViewHolder(val binding: ItemProductoCarritoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductoCarritoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        val prod = item.producto

        // Cantidad actual ya seleccionada
        val cantidadActual = selecciones[prod.id] ?: 0

        // 1. CÁLCULO DEL LÍMITE REAL
        // El límite es lo menor entre: Lo que hay en almacén vs Lo que tienes permitido pedir
        val limiteReal = minOf(prod.stock, item.dotacion.cantidadDiaria)

        holder.binding.apply {
            tvNombre.text = prod.descripcion
            tvPresentacion.text = prod.presentacion

            // 2. TEXTO VISUAL (Solo mostramos lo disponible para el usuario)
            tvStock.text = "Disponible para pedir: $limiteReal"

            tvCantidad.text = cantidadActual.toString()

            // Cargar Foto
            imgProducto.load(prod.imageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_report_image)
                transformations(RoundedCornersTransformation(16f))
            }

            // 3. LOGICA DEL BOTÓN AGREGAR (+) CON TOASTS
            btnAdd.setOnClickListener {
                if (cantidadActual < limiteReal) {
                    // Si aún no llego al límite, agrego uno
                    selecciones[prod.id] = cantidadActual + 1
                    notifyItemChanged(position) // Refrescar vista
                    onCantidadChanged()         // Avisar a la Activity
                } else {
                    // 4. FEEDBACK AL USUARIO (Explicar por qué no puede pedir más)
                    val context = holder.itemView.context
                    if (prod.stock < item.dotacion.cantidadDiaria) {
                        // Caso A: Se acabó en el almacén
                        android.widget.Toast.makeText(context, "Stock insuficiente en almacén (Solo hay $limiteReal)", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        // Caso B: Ya alcanzó su cupo diario
                        android.widget.Toast.makeText(context, "Has alcanzado tu dotación diaria permitida ($limiteReal)", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Botón Restar (-)
            btnRemove.setOnClickListener {
                if (cantidadActual > 0) {
                    selecciones[prod.id] = cantidadActual - 1
                    notifyItemChanged(position)
                    onCantidadChanged()
                }
            }

            // Cambiar color del número si ya seleccionó algo
            if (cantidadActual > 0) {
                tvCantidad.setTextColor(holder.itemView.context.getColor(R.color.brand_primary))
            } else {
                tvCantidad.setTextColor(holder.itemView.context.getColor(R.color.text_primary))
            }
        }
    }

    override fun getItemCount() = lista.size

    fun updateData(nuevaLista: List<DotacionProducto>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}