package com.aplicaion.minimarketapp

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.utils.formatSoles
import com.aplicaion.minimarketapp.viewmodel.CarritoViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class CarritoActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvCarrito: RecyclerView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvIgv: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnContinuar: MaterialButton

    private lateinit var adapter: CarritoAdapter
    private val carritoViewModel = CarritoViewModel.getInstance()

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
        btnContinuar = findViewById(R.id.btnContinuar)

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = CarritoAdapter(
            items = emptyList(),
            onModificarCantidad = { productoId, delta ->
                val prod = carritoViewModel.items.value.find { it.producto.id == productoId }?.producto
                if (prod != null) {
                    if (delta > 0) {
                        carritoViewModel.agregar(prod)
                    } else {
                        carritoViewModel.reducir(prod)
                    }
                }
            },
            onEliminar = { productoId ->
                carritoViewModel.eliminarPorId(productoId)
            }
        )
        rvCarrito.layoutManager = LinearLayoutManager(this)
        rvCarrito.adapter = adapter
    }

    private fun setupListeners() {
        btnContinuar.setOnClickListener {
            val items = carritoViewModel.items.value
            if (items.isEmpty()) {
                Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dialogPago = DialogPagoFragment()
            dialogPago.show(supportFragmentManager, "DialogPagoFragment")
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            carritoViewModel.items.collect { items ->
                adapter.updateItems(items)
                val sub = items.sumOf { it.producto.precioVenta * it.cantidad }
                val igv = sub * 0.18
                val tot = sub + igv

                tvSubtotal.text = sub.formatSoles()
                tvIgv.text = igv.formatSoles()
                tvTotal.text = tot.formatSoles()

                btnContinuar.isEnabled = items.isNotEmpty()
            }
        }
    }
}
