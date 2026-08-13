package com.aplicaion.minimarketapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.api.MinimarketApiProvider
import com.aplicaion.minimarketapp.utils.Constants
import com.aplicaion.minimarketapp.utils.SessionManager
import com.aplicaion.minimarketapp.utils.formatSoles
import com.aplicaion.minimarketapp.viewmodel.CarritoViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class CarritoActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvCarrito: RecyclerView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvIgv: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnContinuar: MaterialButton
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var btnEscanearCarrito: MaterialButton
    private lateinit var cardEscaneoRapido: View

    private lateinit var adapter: CarritoAdapter
    private val carritoViewModel = CarritoViewModel.getInstance()
    private val api by lazy { MinimarketApiProvider.getApi(this) }
    private lateinit var sessionManager: SessionManager

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val codigo = result.data?.getStringExtra(Constants.CODIGO_SCANEADO)
                if (!codigo.isNullOrEmpty()) {
                    buscarYAgregarProductoPorCodigo(codigo)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carrito)

        sessionManager = SessionManager.getInstance(this)

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
        bottomNavigation = findViewById(R.id.bottomNavigation)
        btnEscanearCarrito = findViewById(R.id.btnEscanearCarrito)
        cardEscaneoRapido = findViewById(R.id.cardEscaneoRapido)

        toolbar.inflateMenu(R.menu.menu_carrito)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        bottomNavigation.selectedItemId = R.id.nav_carrito
    }

    private fun setupRecyclerView() {
        adapter = CarritoAdapter(
            items = emptyList(),
            onModificarCantidad = { productoId, delta ->
                if (delta > 0) {
                    val exito = carritoViewModel.aumentarPorId(productoId)
                    if (!exito) {
                        Toast.makeText(this, "Stock máximo disponible alcanzado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    carritoViewModel.reducirPorId(productoId)
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
        val abrirEscaner = {
            val intent = Intent(this, ScannerActivity::class.java)
            scannerLauncher.launch(intent)
        }

        btnEscanearCarrito.setOnClickListener { abrirEscaner() }
        cardEscaneoRapido.setOnClickListener { abrirEscaner() }

        toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_scanner) {
                abrirEscaner()
                true
            } else {
                false
            }
        }

        btnContinuar.setOnClickListener {
            val items = carritoViewModel.items.value
            if (items.isEmpty()) {
                Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dialogPago = DialogPagoFragment()
            dialogPago.show(supportFragmentManager, "DialogPagoFragment")
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
                R.id.nav_carrito -> true
                R.id.nav_inventario -> {
                    if (sessionManager.isAdmin) {
                        val intent = Intent(this, RegistroProductoActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                        finish()
                    } else {
                        Toast.makeText(this, "Acceso exclusivo para Administrador", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.nav_historial -> {
                    val intent = Intent(this, HistorialVentaActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
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

    private fun buscarYAgregarProductoPorCodigo(codigo: String) {
        lifecycleScope.launch {
            val response = api.getProductoPorCodigo(codigo)
            if (response.isSuccess) {
                val prod = response.getOrNull()
                if (prod != null) {
                    val exito = carritoViewModel.agregar(prod)
                    if (exito) {
                        Toast.makeText(this@CarritoActivity, "✓ Escaneado: ${prod.nombre}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@CarritoActivity, "Sin stock disponible para ${prod.nombre}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this@CarritoActivity, "Producto con código $codigo no encontrado", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            carritoViewModel.items.collect { items ->
                adapter.updateItems(items)

                val totalCalculado = items.sumOf { it.producto.precioVenta * it.cantidad }
                val subtotalCalculado = totalCalculado / 1.18
                val igvCalculado = totalCalculado - subtotalCalculado

                tvSubtotal.text = subtotalCalculado.formatSoles()
                tvIgv.text = igvCalculado.formatSoles()
                tvTotal.text = totalCalculado.formatSoles()

                btnContinuar.isEnabled = items.isNotEmpty()
            }
        }
    }
}
