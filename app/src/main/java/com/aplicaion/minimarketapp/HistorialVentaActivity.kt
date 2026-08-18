package com.aplicaion.minimarketapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.util.Pair
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.api.JsonDatabaseManager
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Venta
import com.aplicaion.minimarketapp.repository.ReporteRepository
import com.aplicaion.minimarketapp.utils.SessionManager
import com.aplicaion.minimarketapp.utils.formatSoles
import com.aplicaion.minimarketapp.viewmodel.ReporteViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
    private lateinit var sessionManager: SessionManager
    private var currentVentasList: List<Venta> = emptyList()

    private val reporteViewModel: ReporteViewModel by viewModels {
        val db = AppDatabase.getInstance(this)
        val repo = ReporteRepository(db.ventaDao())
        ReporteViewModel.Factory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_venta)

        sessionManager = SessionManager.getInstance(this)

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

        toolbar.inflateMenu(R.menu.menu_historial)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_exportar_excel -> {
                    exportarReporteExcel()
                    true
                }
                R.id.action_limpiar_historial -> {
                    if (sessionManager.isAdmin) {
                        confirmarVaciarHistorial()
                    } else {
                        Toast.makeText(this, "Solo el Administrador puede vaciar el historial", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }

        toolbar.subtitle = if (sessionManager.isAdmin) "Reportes y Ventas (Admin)" else "Historial de Ventas"
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
                    val intent = Intent(this, punto_venta::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_carrito -> {
                    val intent = Intent(this, CarritoActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_inventario -> {
                    val intent = Intent(this, InventarioMainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_historial -> true
                R.id.nav_proveedores -> {
                    val intent = Intent(this, ProveedorActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = VentaAdapter(
            list = emptyList(),
            onItemClick = { venta ->
                mostrarDetallesVenta(venta)
            },
            onDeleteClick = { venta ->
                confirmarEliminarVenta(venta)
            }
        )
        rvHistorial.layoutManager = LinearLayoutManager(this)
        rvHistorial.adapter = adapter
    }

    private fun observeViewModel() {
        reporteViewModel.ventas.observe(this) { list ->
            currentVentasList = list
            adapter.updateVentas(list)

            val ventasActivas = list.filter { it.estado != "INHABILITADA" && it.estado != "ANULADA" }
            val total = ventasActivas.sumOf { it.total }
            val count = ventasActivas.size
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
            sb.append("Método de Pago: ${venta.metodoPago}\n")
            sb.append("Estado: ${venta.estado}\n\n")
            sb.append("--- PRODUCTOS VENDIDOS ---\n")

            detalles.forEach { d ->
                val prod = withContext(Dispatchers.IO) {
                    db.productoDao().getByIdSync(d.productoId)
                }
                val nombre = prod?.nombre ?: "Producto #${d.productoId}"
                val cantStr = if (d.cantidad % 1.0 == 0.0) "${d.cantidad.toInt()}" else String.format(Locale.US, "%.2f kg", d.cantidad)
                sb.append("• $nombre x$cantStr = ${d.subtotalLinea.formatSoles()}\n")
            }

            sb.append("\nTOTAL: ${venta.total.formatSoles()}")

            val builder = AlertDialog.Builder(this@HistorialVentaActivity)
                .setTitle("Detalle de Venta #${venta.codigoVenta}")
                .setMessage(sb.toString())
                .setPositiveButton("Cerrar", null)

            if (sessionManager.isAdmin) {
                builder.setNeutralButton("🗑️ Eliminar Venta") { _, _ ->
                    confirmarEliminarVenta(venta)
                }
            }

            builder.show()
        }
    }

    private fun confirmarEliminarVenta(venta: Venta) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Venta")
            .setMessage("¿Deseas eliminar permanentemente la venta '${venta.codigoVenta}'?\n\nLos productos vendidos serán restituidos al stock del inventario.")
            .setPositiveButton("Eliminar") { _, _ ->
                ejecutarEliminacionVenta(venta)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun ejecutarEliminacionVenta(venta: Venta) {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@HistorialVentaActivity)
            withContext(Dispatchers.IO) {
                // Devolver stock
                val detalles = db.detalleVentaDao().getByVentaIdList(venta.id)
                for (d in detalles) {
                    val p = db.productoDao().getByIdSync(d.productoId)
                    if (p != null) {
                        val stockRestaurado = (p.stock + d.cantidad).toInt()
                        db.productoDao().actualizarStock(p.id, stockRestaurado)
                    }
                }
                // Eliminar detalles y venta
                db.detalleVentaDao().deleteByVentaId(venta.id)
                db.ventaDao().delete(venta)
                JsonDatabaseManager.syncAndSaveJsonFile(this@HistorialVentaActivity, db)
            }
            Toast.makeText(this@HistorialVentaActivity, "✓ Venta eliminada y stock restituido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmarVaciarHistorial() {
        AlertDialog.Builder(this)
            .setTitle("Vaciar Todo el Historial")
            .setMessage("¿Estás seguro de vaciar todo el registro de ventas del historial?\nEsta acción no se puede deshacer.")
            .setPositiveButton("Vaciar Todo") { _, _ ->
                lifecycleScope.launch {
                    val db = AppDatabase.getInstance(this@HistorialVentaActivity)
                    withContext(Dispatchers.IO) {
                        db.detalleVentaDao().deleteAll()
                        db.ventaDao().deleteAll()
                        JsonDatabaseManager.syncAndSaveJsonFile(this@HistorialVentaActivity, db)
                    }
                    Toast.makeText(this@HistorialVentaActivity, "Historial vaciado completamente", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportarReporteExcel() {
        if (currentVentasList.isEmpty()) {
            Toast.makeText(this, "No hay ventas registradas para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(this@HistorialVentaActivity)
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val sb = StringBuilder()

                sb.append("\uFEFF") // UTF-8 BOM para Excel
                sb.append("Codigo Venta,Fecha,Metodo de Pago,Subtotal (S/),IGV (S/),Total (S/),Estado,Detalle Productos\n")

                for (v in currentVentasList) {
                    val prodsText = withContext(Dispatchers.IO) {
                        val detalles = db.detalleVentaDao().getByVentaIdList(v.id)
                        val listFormatted = mutableListOf<String>()
                        for (d in detalles) {
                            val p = db.productoDao().getByIdSync(d.productoId)
                            val cantStr = if (d.cantidad % 1.0 == 0.0) "${d.cantidad.toInt()}" else String.format(Locale.US, "%.2f kg", d.cantidad)
                            listFormatted.add("${p?.nombre ?: "Prod"} (x$cantStr)")
                        }
                        listFormatted.joinToString("; ")
                    }
                    val fechaFormateada = sdf.format(Date(v.fecha))
                    sb.append("\"${v.codigoVenta}\",")
                    sb.append("\"$fechaFormateada\",")
                    sb.append("\"${v.metodoPago}\",")
                    sb.append(String.format(Locale.US, "%.2f,", v.subtotal))
                    sb.append(String.format(Locale.US, "%.2f,", v.igv))
                    sb.append(String.format(Locale.US, "%.2f,", v.total))
                    sb.append("\"${v.estado}\",")
                    sb.append("\"${prodsText.replace("\"", "'")}\"\n")
                }

                val fileName = "Reporte_Ventas_${System.currentTimeMillis()}.csv"
                val file = File(cacheDir, fileName)
                val fos = FileOutputStream(file)
                fos.write(sb.toString().toByteArray(Charsets.UTF_8))
                fos.flush()
                fos.close()

                val uri: Uri = FileProvider.getUriForFile(
                    this@HistorialVentaActivity,
                    "${applicationContext.packageName}.provider",
                    file
                )

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "Reporte de Ventas - Minimarket")
                    putExtra(Intent.EXTRA_TEXT, "Adjunto reporte de ventas exportado desde el sistema de Minimarket.")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(sendIntent, "Exportar / Compartir Reporte"))

            } catch (e: Exception) {
                Toast.makeText(this@HistorialVentaActivity, "Error al generar reporte: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    class VentaAdapter(
        private var list: List<Venta>,
        private val onItemClick: (Venta) -> Unit,
        private val onDeleteClick: (Venta) -> Unit
    ) : RecyclerView.Adapter<VentaAdapter.VentaViewHolder>() {

        class VentaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvCodigoVenta: TextView = view.findViewById(R.id.tvCodigoVenta)
            val tvTotalVenta: TextView = view.findViewById(R.id.tvTotalVenta)
            val tvFecha: TextView = view.findViewById(R.id.tvFecha)
            val tvMetodoPago: TextView = view.findViewById(R.id.tvMetodoPago)
            val tvEstadoVenta: TextView = view.findViewById(R.id.tvEstadoVenta)
            val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminarVentaItem)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VentaViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_venta, parent, false)
            return VentaViewHolder(view)
        }

        override fun onBindViewHolder(holder: VentaViewHolder, position: Int) {
            val venta = list[position]
            val context = holder.itemView.context
            holder.tvCodigoVenta.text = venta.codigoVenta
            holder.tvTotalVenta.text = venta.total.formatSoles()
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            holder.tvFecha.text = sdf.format(Date(venta.fecha))

            val estado = venta.estado.uppercase(Locale.getDefault())
            holder.tvEstadoVenta.text = estado
            if (estado == "INHABILITADA" || estado == "ANULADA") {
                holder.tvEstadoVenta.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.rojo_alerta))
                holder.tvTotalVenta.setTextColor(ContextCompat.getColor(context, R.color.texto_secundario))
                holder.itemView.alpha = 0.7f
            } else {
                holder.tvEstadoVenta.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.verde_nav))
                holder.tvTotalVenta.setTextColor(ContextCompat.getColor(context, R.color.verde_ganancia))
                holder.itemView.alpha = 1.0f
            }

            val metodo = venta.metodoPago.uppercase(Locale.getDefault())
            holder.tvMetodoPago.text = metodo

            val colorRes = when {
                metodo.contains("YAPE") -> R.color.color_yape
                metodo.contains("PLIN") -> R.color.color_plin
                metodo.contains("TARJETA") -> R.color.color_tarjeta
                else -> R.color.color_efectivo
            }

            holder.tvMetodoPago.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))

            holder.itemView.setOnClickListener {
                onItemClick(venta)
            }

            holder.btnEliminar.setOnClickListener {
                onDeleteClick(venta)
            }
        }

        override fun getItemCount(): Int = list.size

        fun updateVentas(newList: List<Venta>) {
            list = newList
            notifyDataSetChanged()
        }
    }
}
