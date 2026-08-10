package com.aplicaion.minimarketapp

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.repository.VentaRepository
import com.aplicaion.minimarketapp.utils.Constants
import com.aplicaion.minimarketapp.utils.Resource
import com.aplicaion.minimarketapp.utils.formatSoles
import com.aplicaion.minimarketapp.viewmodel.VentaViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class CarritoActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvCarrito: RecyclerView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvIgv: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnEfectivo: MaterialButton
    private lateinit var btnYape: MaterialButton
    private lateinit var btnConfirmarVenta: MaterialButton

    private lateinit var adapter: CarritoAdapter
    private var metodoPagoSeleccionado: String = Constants.PAGO_EFECTIVO

    private val ventaViewModel: VentaViewModel by viewModels {
        val db = AppDatabase.getInstance(this)
        val ventaRepo = VentaRepository(db.ventaDao(), db.detalleVentaDao(), db.productoDao())
        VentaViewModel.Factory(ventaRepo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carrito)

        initViews()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvCarrito = findViewById(R.id.rvCarrito)
        tvSubtotal = findViewById(R.id.tvSubtotal)
        tvIgv = findViewById(R.id.tvIgv)
        tvTotal = findViewById(R.id.tvTotal)
        btnEfectivo = findViewById(R.id.btnEfectivo)
        btnYape = findViewById(R.id.btnYape)
        btnConfirmarVenta = findViewById(R.id.btnConfirmarVenta)

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = CarritoAdapter(
            items = emptyList(),
            onModificarCantidad = { id, delta ->
                ventaViewModel.modificarCantidad(id, delta)
            },
            onEliminar = { id ->
                ventaViewModel.eliminarDelCarrito(id)
            }
        )
        rvCarrito.layoutManager = LinearLayoutManager(this)
        rvCarrito.adapter = adapter
    }

    private fun setupListeners() {
        btnEfectivo.setOnClickListener {
            metodoPagoSeleccionado = Constants.PAGO_EFECTIVO
            btnEfectivo.alpha = 1.0f
            btnYape.alpha = 0.5f
            Toast.makeText(this, "Método de pago: Efectivo", Toast.LENGTH_SHORT).show()
        }

        btnYape.setOnClickListener {
            metodoPagoSeleccionado = Constants.PAGO_YAPE
            btnEfectivo.alpha = 0.5f
            btnYape.alpha = 1.0f
            Toast.makeText(this, "Método de pago: Yape", Toast.LENGTH_SHORT).show()
        }

        btnConfirmarVenta.setOnClickListener {
            ventaViewModel.procesarVenta(metodoPagoSeleccionado)
        }
    }

    private fun observeViewModel() {
        ventaViewModel.carrito.observe(this) { items ->
            adapter.updateItems(items)
        }

        ventaViewModel.subtotal.observe(this) { sub ->
            tvSubtotal.text = sub.formatSoles()
        }

        ventaViewModel.igv.observe(this) { igv ->
            tvIgv.text = igv.formatSoles()
        }

        ventaViewModel.total.observe(this) { tot ->
            tvTotal.text = tot.formatSoles()
        }

        ventaViewModel.ventaResult.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    btnConfirmarVenta.isEnabled = false
                }
                is Resource.Success -> {
                    btnConfirmarVenta.isEnabled = true
                    Toast.makeText(this, resource.data ?: "Venta realizada exitosamente", Toast.LENGTH_LONG).show()
                    ventaViewModel.resetVentaResult()
                    finish()
                }
                is Resource.Error -> {
                    btnConfirmarVenta.isEnabled = true
                    Toast.makeText(this, resource.message ?: "Error al realizar venta", Toast.LENGTH_LONG).show()
                    ventaViewModel.resetVentaResult()
                }
                null -> {
                    btnConfirmarVenta.isEnabled = true
                }
            }
        }
    }
}
