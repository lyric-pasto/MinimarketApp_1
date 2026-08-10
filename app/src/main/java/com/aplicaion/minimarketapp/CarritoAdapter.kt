package com.aplicaion.minimarketapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.repository.ItemCarrito
import com.aplicaion.minimarketapp.utils.formatSoles
import com.google.android.material.button.MaterialButton

class CarritoAdapter(
    private var items: List<ItemCarrito>,
    private val onModificarCantidad: (productoId: Int, delta: Int) -> Unit,
    private val onEliminar: (productoId: Int) -> Unit
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    class CarritoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
        items = newList
        notifyDataSetChanged()
    }
}
