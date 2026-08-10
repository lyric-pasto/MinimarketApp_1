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
import com.aplicaion.minimarketapp.db.entity.Venta
import com.aplicaion.minimarketapp.repository.ReporteRepository
import com.aplicaion.minimarketapp.utils.formatSoles
import com.aplicaion.minimarketapp.viewmodel.ReporteViewModel
import com.google.android.material.appbar.MaterialToolbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistorialVentaActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvHistorial: RecyclerView
    private lateinit var adapter: VentaAdapter

    private val reporteViewModel: ReporteViewModel by viewModels {
        val db = AppDatabase.getInstance(this)
        val repo = ReporteRepository(db.ventaDao())
        ReporteViewModel.Factory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_venta)

        toolbar = findViewById(R.id.toolbar)
        rvHistorial = findViewById(R.id.rvHistorial)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        adapter = VentaAdapter(emptyList())
        rvHistorial.layoutManager = LinearLayoutManager(this)
        rvHistorial.adapter = adapter

        reporteViewModel.ventas.observe(this) { list ->
            adapter.updateVentas(list)
        }
    }

    class VentaAdapter(private var list: List<Venta>) :
        RecyclerView.Adapter<VentaAdapter.VentaViewHolder>() {

        class VentaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvCodigoVenta: TextView = view.findViewById(R.id.tvCodigoVenta)
            val tvTotalVenta: TextView = view.findViewById(R.id.tvTotalVenta)
            val tvFecha: TextView = view.findViewById(R.id.tvFecha)
            val tvMetodoPago: TextView = view.findViewById(R.id.tvMetodoPago)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VentaViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_venta, parent, false)
            return VentaViewHolder(view)
        }

        override fun onBindViewHolder(holder: VentaViewHolder, position: Int) {
            val venta = list[position]
            holder.tvCodigoVenta.text = venta.codigoVenta
            holder.tvTotalVenta.text = venta.total.formatSoles()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            holder.tvFecha.text = sdf.format(Date(venta.fecha))
            holder.tvMetodoPago.text = venta.metodoPago
        }

        override fun getItemCount(): Int = list.size

        fun updateVentas(newList: List<Venta>) {
            list = newList
            notifyDataSetChanged()
        }
    }
}
