package com.aplicaion.minimarketapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.utils.formatSoles
import com.aplicaion.minimarketapp.viewmodel.ItemCarrito
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton

class CarritoAdapter(
    private var items: List<ItemCarrito>,
    private val onModificarCantidad: (productoId: Int, delta: Int) -> Unit,
    private val onEliminar: (productoId: Int) -> Unit
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    class CarritoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProducto: ImageView = view.findViewById(R.id.ivProducto)
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecio)
        val tvSubtotalLinea: TextView = view.findViewById(R.id.tvSubtotalLinea)
        val tvCantidad: TextView = view.findViewById(R.id.tvCantidad)
        val btnRestar: MaterialButton = view.findViewById(R.id.btnRestar)
        val btnSumar: MaterialButton = view.findViewById(R.id.btnSumar)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrito, parent, false)
        return CarritoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) {
        val item = items[position]
        holder.tvNombre.text = item.producto.nombre
        holder.tvPrecio.text = "${item.producto.precioVenta.formatSoles()} c/u"
        holder.tvSubtotalLinea.text = "Subtotal: ${item.subtotalLinea.formatSoles()}"
        holder.tvCantidad.text = item.cantidad.toString()

        if (!item.producto.imagenPath.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.producto.imagenPath)
                .placeholder(R.drawable.ic_product_placeholder)
                .error(R.drawable.ic_product_placeholder)
                .centerCrop()
                .into(holder.ivProducto)
        } else {
            holder.ivProducto.setImageResource(R.drawable.ic_product_placeholder)
        }

        // Deshabilitar botón + si ya alcanzó el stock máximo
        val alcanzoMaximo = item.cantidad >= item.producto.stock
        holder.btnSumar.alpha = if (alcanzoMaximo) 0.5f else 1.0f

        holder.btnRestar.setOnClickListener {
            onModificarCantidad(item.producto.id, -1)
        }

        holder.btnSumar.setOnClickListener {
            onModificarCantidad(item.producto.id, 1)
        }

        holder.btnEliminar.setOnClickListener {
            onEliminar(item.producto.id)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newList: List<ItemCarrito>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = items.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return items[oldPos].producto.id == newList[newPos].producto.id
            }

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                val old = items[oldPos]
                val new = newList[newPos]
                return old.cantidad == new.cantidad &&
                        old.producto.precioVenta == new.producto.precioVenta &&
                        old.producto.stock == new.producto.stock &&
                        old.producto.nombre == new.producto.nombre
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items = ArrayList(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}
