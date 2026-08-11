package com.aplicaion.minimarketapp

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.utils.formatSoles
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton

class ProductAdapter(
    private var productos: List<Producto>,
    private val onAgregarClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProducto: ImageView = view.findViewById(R.id.ivProducto)
        val tvNombreProducto: TextView = view.findViewById(R.id.tvNombreProducto)
        val tvCategoria: TextView = view.findViewById(R.id.tvCategoria)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecio)
        val btnAgregar: MaterialButton = view.findViewById(R.id.btnAgregar)
        val ivDetalle: ImageView = view.findViewById(R.id.ivDetalle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val prod = productos[position]
        holder.tvNombreProducto.text = prod.nombre
        holder.tvPrecio.text = prod.precioVenta.formatSoles()

        // Stock alert badge check: if stock < 5, highlight or append low stock notice
        val context = holder.itemView.context
        if (prod.stock < 5) {
            holder.tvCategoria.text = "Stock: ${prod.stock} (¡ALERTA STOCK BAJO!)"
            holder.tvCategoria.setTextColor(ContextCompat.getColor(context, R.color.rojo_alerta))
        } else {
            holder.tvCategoria.text = "Stock: ${prod.stock} unds"
            holder.tvCategoria.setTextColor(ContextCompat.getColor(context, R.color.texto_secundario))
        }

        if (!prod.imagenPath.isNull_or_blank_safe()) {
            Glide.with(holder.itemView.context)
                .load(prod.imagenPath)
                .placeholder(R.drawable.ic_product_placeholder)
                .error(R.drawable.ic_product_placeholder)
                .centerCrop()
                .into(holder.ivProducto)
        } else {
            holder.ivProducto.setImageResource(R.drawable.ic_product_placeholder)
        }

        holder.btnAgregar.setOnClickListener {
            onAgregarClick(prod)
        }

        holder.ivDetalle.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, RegistroProductoActivity::class.java)
            intent.putExtra("PRODUCTO_ID", prod.id.toLong())
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = productos.size

    fun updateProductos(newList: List<Producto>) {
        productos = newList
        notifyDataSetChanged()
    }

    private fun String?.isNull_or_blank_safe(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
