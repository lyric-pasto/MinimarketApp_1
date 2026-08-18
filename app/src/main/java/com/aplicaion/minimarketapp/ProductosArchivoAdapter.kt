package com.aplicaion.minimarketapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.utils.formatSoles
import com.bumptech.glide.Glide

class ProductosArchivoAdapter(
    private var productos: List<Producto>,
    private val onEditarClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductosArchivoAdapter.ArchivoViewHolder>() {

    class ArchivoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProducto: ImageView = view.findViewById(R.id.ivProductoArchivo)
        val tvNombre: TextView = view.findViewById(R.id.tvNombreProductoArchivo)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecioProductoArchivo)
        val tvStock: TextView = view.findViewById(R.id.tvStockProductoArchivo)
        val tvTipoVenta: TextView = view.findViewById(R.id.tvTipoVentaBadgeArchivo)
        val btnEditar: ImageButton = view.findViewById(R.id.btnEditarProductoArchivo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchivoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto_archivo, parent, false)
        return ArchivoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArchivoViewHolder, position: Int) {
        val prod = productos[position]
        val context = holder.itemView.context

        holder.tvNombre.text = prod.nombre
        val esPorPeso = prod.esPorPeso || prod.tipoVenta == "PESO" || prod.unidadMedida == "KG"
        if (esPorPeso) {
            holder.tvPrecio.text = "${prod.precioVenta.formatSoles()} / kg"
            holder.tvTipoVenta.visibility = View.VISIBLE
            holder.tvTipoVenta.text = "⚖️ KG"
        } else {
            holder.tvPrecio.text = "${prod.precioVenta.formatSoles()} c/u"
            holder.tvTipoVenta.visibility = View.GONE
        }

        holder.tvStock.text = "Stock: ${prod.stock} u"

        if (!prod.imagenPath.isNullOrBlank()) {
            Glide.with(context)
                .load(prod.imagenPath)
                .placeholder(R.drawable.ic_product_placeholder)
                .error(R.drawable.ic_product_placeholder)
                .centerCrop()
                .into(holder.ivProducto)
        } else {
            holder.ivProducto.setImageResource(R.drawable.ic_product_placeholder)
        }

        holder.btnEditar.setOnClickListener {
            onEditarClick(prod)
        }
    }

    override fun getItemCount(): Int = productos.size

    fun updateData(newList: List<Producto>) {
        productos = newList
        notifyDataSetChanged()
    }
}
