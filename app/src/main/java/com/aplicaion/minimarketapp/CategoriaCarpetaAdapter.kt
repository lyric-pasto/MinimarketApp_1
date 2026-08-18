package com.aplicaion.minimarketapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.db.entity.Categoria
import com.google.android.material.card.MaterialCardView

data class CategoriaConConteo(
    val categoria: Categoria,
    val cantidadProductos: Int
)

class CategoriaCarpetaAdapter(
    private var items: List<CategoriaConConteo>,
    private val onCarpetaClick: (Categoria) -> Unit,
    private val onEditarClick: (Categoria) -> Unit
) : RecyclerView.Adapter<CategoriaCarpetaAdapter.CarpetaViewHolder>() {

    class CarpetaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardCategoriaCarpeta)
        val ivIconoCarpeta: ImageView = view.findViewById(R.id.ivIconoCarpeta)
        val tvNombreCategoria: TextView = view.findViewById(R.id.tvNombreCategoria)
        val tvConteoProductos: TextView = view.findViewById(R.id.tvConteoProductos)
        val btnEditar: ImageButton = view.findViewById(R.id.btnEditarCategoria)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarpetaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_categoria_carpeta, parent, false)
        return CarpetaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarpetaViewHolder, position: Int) {
        val item = items[position]
        val cat = item.categoria

        holder.tvNombreCategoria.text = cat.nombre
        holder.tvConteoProductos.text = "${item.cantidadProductos} productos"

        holder.card.setOnClickListener {
            onCarpetaClick(cat)
        }

        holder.btnEditar.setOnClickListener {
            onEditarClick(cat)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newList: List<CategoriaConConteo>) {
        items = newList
        notifyDataSetChanged()
    }
}
