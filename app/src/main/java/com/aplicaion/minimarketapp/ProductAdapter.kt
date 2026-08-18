package com.aplicaion.minimarketapp

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
import com.google.android.material.card.MaterialCardView

class ProductAdapter(
    private var productos: List<Producto>,
    private val onProductoClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardProductoItem)
        val ivProducto: ImageView = view.findViewById(R.id.ivProductoGrid)
        val tvBadgeTipoVenta: TextView = view.findViewById(R.id.tvBadgeTipoVenta)
        val tvBadgeAlertaStock: TextView = view.findViewById(R.id.tvBadgeAlertaStock)
        val tvNombreProducto: TextView = view.findViewById(R.id.tvNombreProductoGrid)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecioGrid)
        val tvStock: TextView = view.findViewById(R.id.tvStockGrid)
        val btnAccion: MaterialButton = view.findViewById(R.id.btnAccionGrid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto_grid, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val prod = productos[position]
        val context = holder.itemView.context

        holder.tvNombreProducto.text = prod.nombre
        val esVentaPorPeso = prod.esPorPeso || prod.tipoVenta == "PESO" || prod.unidadMedida == "KG"

        if (esVentaPorPeso) {
            holder.tvPrecio.text = "${prod.precioVenta.formatSoles()}/kg"
            holder.tvBadgeTipoVenta.visibility = View.VISIBLE
            holder.tvBadgeTipoVenta.text = "⚖️ KG"
            holder.btnAccion.text = "⚖️ Pesar"
        } else {
            holder.tvPrecio.text = prod.precioVenta.formatSoles()
            holder.tvBadgeTipoVenta.visibility = View.GONE
            holder.btnAccion.text = "+ Añadir"
        }

        // Alerta de stock
        if (prod.stock < 5) {
            holder.tvStock.text = "${prod.stock} u (¡Bajo!)"
            holder.tvStock.setTextColor(ContextCompat.getColor(context, R.color.rojo_alerta))
            holder.tvBadgeAlertaStock.visibility = View.VISIBLE
        } else {
            holder.tvStock.text = "${prod.stock} u"
            holder.tvStock.setTextColor(ContextCompat.getColor(context, R.color.texto_secundario))
            holder.tvBadgeAlertaStock.visibility = View.GONE
        }

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

        val clickListener = View.OnClickListener {
            onProductoClick(prod)
        }

        holder.card.setOnClickListener(clickListener)
        holder.btnAccion.setOnClickListener(clickListener)
    }

    override fun getItemCount(): Int = productos.size

    fun updateProductos(newList: List<Producto>) {
        productos = newList
        notifyDataSetChanged()
    }
}
