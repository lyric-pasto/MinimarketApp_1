package com.aplicaion.minimarketapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Venta
import com.aplicaion.minimarketapp.repository.ReporteRepository
import com.aplicaion.minimarketapp.utils.formatSoles
import com.aplicaion.minimarketapp.viewmodel.ReporteViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistorialVentaActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvHistorial: RecyclerView
    private lateinit var etBuscarVenta: EditText
    private lateinit var tvTotalReporte: TextView
    private lateinit var tvCantidadVentas: TextView
    private lateinit var tvTicketPromedio: TextView
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var btnFiltroHoy: MaterialButton
    private lateinit var btnFiltroSemana: MaterialButton
    private lateinit var btnFiltroMes: MaterialButton
    private lateinit var btnFiltroTodo: MaterialButton
    private lateinit var btnRangoFechas: MaterialButton

    private lateinit var btnPagoTodos: MaterialButton
    private lateinit var btnPagoEfectivo: MaterialButton
    private lateinit var btnPagoYape: MaterialButton
    private lateinit var btnPagoPlin: MaterialButton
    private lateinit var btnPagoTarjeta: MaterialButton

    private lateinit var adapter: VentaAdapter

    private val reporteViewModel: ReporteViewModel by viewModels {
        val db = AppDatabase.getInstance(this)
        val repo = ReporteRepository(db.ventaDao())
        ReporteViewModel.Factory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_venta)

        initViews()
        setupListeners()
        setupRecyclerView()
        observeViewModel()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvHistorial = findViewById(R.id.rvHistorial)
        etBuscarVenta = findViewById(R.id.etBuscarVenta)
        tvTotalReporte = findViewById(R.id.tvTotalReporte)
        tvCantidadVentas = findViewById(R.id.tvCantidadVentas)
        tvTicketPromedio = findViewById(R.id.tvTicketPromedio)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        btnFiltroHoy = findViewById(R.id.btnFiltroHoy)
        btnFiltroSemana = findViewById(R.id.btnFiltroSemana)
        btnFiltroMes = findViewById(R.id.btnFiltroMes)
        btnFiltroTodo = findViewById(R.id.btnFiltroTodo)
        btnRangoFechas = findViewById(R.id.btnRangoFechas)

        btnPagoTodos = findViewById(R.id.btnPagoTodos)
        btnPagoEfectivo = findViewById(R.id.btnPagoEfectivo)
        btnPagoYape = findViewById(R.id.btnPagoYape)
        btnPagoPlin = findViewById(R.id.btnPagoPlin)
        btnPagoTarjeta = findViewById(R.id.btnPagoTarjeta)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        bottomNavigation.selectedItemId = R.id.nav_historial
    }

    private fun setupListeners() {
        // Buscador de Ventas
        etBuscarVenta.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                reporteViewModel.buscarPorQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Filtros Rápidos de Fecha
        val btnFechas = listOf(btnFiltroHoy, btnFiltroSemana, btnFiltroMes, btnFiltroTodo, btnRangoFechas)

        fun resaltarBotonFecha(btnActivo: MaterialButton) {
            btnFechas.forEach { btn ->
                if (btn == btnActivo) {
                    btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.morado_scanner))
                    btn.setTextColor(Color.WHITE)
                } else {
                    btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.fondo_card))
                    btn.setTextColor(ContextCompat.getColor(this, R.color.texto_principal))
                }
            }
        }

        btnFiltroHoy.setOnClickListener {
            resaltarBotonFecha(btnFiltroHoy)
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val inicio = cal.timeInMillis
            val fin = System.currentTimeMillis()
            reporteViewModel.filtrarPorFecha(inicio, fin)
        }

        btnFiltroSemana.setOnClickListener {
            resaltarBotonFecha(btnFiltroSemana)
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -7)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            val inicio = cal.timeInMillis
            val fin = System.currentTimeMillis()
            reporteViewModel.filtrarPorFecha(inicio, fin)
        }

        btnFiltroMes.setOnClickListener {
            resaltarBotonFecha(btnFiltroMes)
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            val inicio = cal.timeInMillis
            val fin = System.currentTimeMillis()
            reporteViewModel.filtrarPorFecha(inicio, fin)
        }

        btnFiltroTodo.setOnClickListener {
            resaltarBotonFecha(btnFiltroTodo)
            reporteViewModel.limpiarFiltros()
        }

        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Seleccionar rango de fechas")
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection: Pair<Long, Long>? ->
            if (selection != null) {
                resaltarBotonFecha(btnRangoFechas)
                val inicio = selection.first
                val fin = selection.second + (86400000L - 1L)
                reporteViewModel.filtrarPorFecha(inicio, fin)
            }
        }

        btnRangoFechas.setOnClickListener {
            dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
        }

        // Filtros Método de Pago
        val btnPagos = listOf(btnPagoTodos, btnPagoEfectivo, btnPagoYape, btnPagoPlin, btnPagoTarjeta)

        fun resaltarBotonPago(btnActivo: MaterialButton) {
            btnPagos.forEach { btn ->
                if (btn == btnActivo) {
                    btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.verde_nav))
                    btn.setTextColor(Color.WHITE)
                } else {
                    btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.fondo_card))
                    btn.setTextColor(ContextCompat.getColor(this, R.color.texto_principal))
                }
            }
        }

        btnPagoTodos.setOnClickListener {
            resaltarBotonPago(btnPagoTodos)
            reporteViewModel.filtrarPorMetodoPago(null)
        }
        btnPagoEfectivo.setOnClickListener {
            resaltarBotonPago(btnPagoEfectivo)
            reporteViewModel.filtrarPorMetodoPago("Efectivo")
        }
        btnPagoYape.setOnClickListener {
            resaltarBotonPago(btnPagoYape)
            reporteViewModel.filtrarPorMetodoPago("Yape")
        }
        btnPagoPlin.setOnClickListener {
            resaltarBotonPago(btnPagoPlin)
            reporteViewModel.filtrarPorMetodoPago("Plin")
        }
        btnPagoTarjeta.setOnClickListener {
            resaltarBotonPago(btnPagoTarjeta)
            reporteViewModel.filtrarPorMetodoPago("Tarjeta")
        }

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_pos -> {
                    val intent = Intent(this, punto_venta::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_carrito -> {
                    val intent = Intent(this, CarritoActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_inventario -> {
                    val intent = Intent(this, RegistroProductoActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_historial -> true
                R.id.nav_proveedores -> {
                    val intent = Intent(this, ProveedorActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = VentaAdapter(emptyList()) { venta ->
            mostrarDetallesVenta(venta)
        }
        rvHistorial.layoutManager = LinearLayoutManager(this)
        rvHistorial.adapter = adapter
    }

    private fun observeViewModel() {
        reporteViewModel.ventas.observe(this) { list ->
            adapter.updateVentas(list)
            val total = list.sumOf { it.total }
            val count = list.size
            val promedio = if (count > 0) total / count else 0.0

            tvTotalReporte.text = total.formatSoles()
            tvCantidadVentas.text = count.toString()
            tvTicketPromedio.text = promedio.formatSoles()
        }
    }

    private fun mostrarDetallesVenta(venta: Venta) {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@HistorialVentaActivity)
            val detalles = withContext(Dispatchers.IO) {
                db.detalleVentaDao().getByVentaIdList(venta.id)
            }

            val sb = StringBuilder()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sb.append("Código: ${venta.codigoVenta}\n")
            sb.append("Fecha: ${sdf.format(Date(venta.fecha))}\n")
            sb.append("Método de Pago: ${venta.metodoPago}\n\n")
            sb.append("--- PRODUCTOS ---\n")

            detalles.forEach { d ->
                val prod = withContext(Dispatchers.IO) {
                    db.productoDao().getByIdSync(d.productoId)
                }
                val nombre = prod?.nombre ?: "Producto #${d.productoId}"
                sb.append("• $nombre x${d.cantidad} = ${d.subtotalLinea.formatSoles()}\n")
            }

            sb.append("\nTOTAL: ${venta.total.formatSoles()}")

            AlertDialog.Builder(this@HistorialVentaActivity)
                .setTitle("Detalle de Venta")
                .setMessage(sb.toString())
                .setPositiveButton("Cerrar", null)
                .show()
        }
    }

    class VentaAdapter(
        private var list: List<Venta>,
        private val onItemClick: (Venta) -> Unit
    ) : RecyclerView.Adapter<VentaAdapter.VentaViewHolder>() {

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

            // Configurar el badge según Método de Pago
            val context = holder.itemView.context
            val metodo = venta.metodoPago.uppercase(Locale.getDefault())
            holder.tvMetodoPago.text = metodo

            val colorRes = when {
                metodo.contains("YAPE") -> R.color.color_yape
                metodo.contains("PLIN") -> R.color.color_plin
                metodo.contains("TARJETA") -> R.color.color_tarjeta
                else -> R.color.color_efectivo
            }

            holder.tvMetodoPago.setBackgroundColor(ContextCompat.getColor(context, colorRes))

            holder.itemView.setOnClickListener {
                onItemClick(venta)
            }
        }

        override fun getItemCount(): Int = list.size

        fun updateVentas(newList: List<Venta>) {
            list = newList
            notifyDataSetChanged()
        }
    }
}
