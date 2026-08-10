package com.aplicaion.minimarketapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Proveedor
import com.aplicaion.minimarketapp.repository.ProveedorRepository
import com.aplicaion.minimarketapp.viewmodel.ProveedorViewModel
import com.google.android.material.appbar.MaterialToolbar

class ProveedorActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvProveedores: RecyclerView
    private lateinit var adapter: ProveedorAdapter

    private val proveedorViewModel: ProveedorViewModel by viewModels {
        val db = AppDatabase.getInstance(this)
        val repo = ProveedorRepository(db.proveedorDao())
        ProveedorViewModel.Factory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proveedor)

        toolbar = findViewById(R.id.toolbar)
        rvProveedores = findViewById(R.id.rvProveedores)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        adapter = ProveedorAdapter(emptyList())
        rvProveedores.layoutManager = LinearLayoutManager(this)
        rvProveedores.adapter = adapter

        proveedorViewModel.proveedores.observe(this) { list ->
            adapter.updateProveedores(list)
        }
    }

    class ProveedorAdapter(private var list: List<Proveedor>) :
        RecyclerView.Adapter<ProveedorAdapter.ProveedorViewHolder>() {

        class ProveedorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNombreProveedor: TextView = view.findViewById(R.id.tvNombreProveedor)
            val tvRuc: TextView = view.findViewById(R.id.tvRuc)
            val tvContacto: TextView = view.findViewById(R.id.tvContacto)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProveedorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_proveedor, parent, false)
            return ProveedorViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProveedorViewHolder, position: Int) {
            val prov = list[position]
            holder.tvNombreProveedor.text = prov.nombre
            holder.tvRuc.text = "RUC: ${prov.ruc}"
            holder.tvContacto.text = "Cel: ${prov.celular} - Correo: ${prov.correo}"
        }

        override fun getItemCount(): Int = list.size

        fun updateProveedores(newList: List<Proveedor>) {
            list = newList
            notifyDataSetChanged()
        }
    }
}
