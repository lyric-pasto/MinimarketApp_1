package com.aplicaion.minimarketapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.repository.CategoriaRepository
import com.aplicaion.minimarketapp.db.dao.DetalleVentaDao
import com.aplicaion.minimarketapp.repository.ItemCarrito
import com.aplicaion.minimarketapp.repository.ProductoRepository
import com.aplicaion.minimarketapp.repository.VentaRepository
import com.aplicaion.minimarketapp.utils.Constants
import com.aplicaion.minimarketapp.utils.Resource
import com.aplicaion.minimarketapp.viewmodel.ProductoViewModel
import com.aplicaion.minimarketapp.viewmodel.VentaViewModel
import com.google.android.material.chip.Chip

class punto_venta : AppCompatActivity() {

    private lateinit var btnVolver: ImageButton
    private lateinit var searchProductos: SearchView
    private lateinit var btnEscanear: ImageButton
    private lateinit var recyclerProductos: RecyclerView

    private lateinit var chipTodos: Chip
    private lateinit var chipAbarrotes: Chip
    private lateinit var chipLacteos: Chip
    private lateinit var chipBebidas: Chip
    private lateinit var chipLimpieza: Chip
    private lateinit var chipSnacks: Chip

    private lateinit var tabCarrito: LinearLayout
    private lateinit var tabInventario: LinearLayout
    private lateinit var tabHistorial: LinearLayout
    private lateinit var tabProveedores: LinearLayout

    private lateinit var productAdapter: ProductAdapter
    private var allProductsList: List<Producto> = emptyList()

    private val productoViewModel: ProductoViewModel by viewModels {
        val db = AppDatabase.getInstance(this)
        val prodRepo = ProductoRepository(db.productoDao())
        val catRepo = CategoriaRepository(db.categoriaDao())
        ProductoViewModel.Factory(prodRepo, catRepo)
    }

    private val ventaViewModel: VentaViewModel by viewModels {
        val db = AppDatabase.getInstance(this)
        val ventaRepo = VentaRepository(db.ventaDao(), db.detalleVentaDao(), db.productoDao())
        VentaViewModel.Factory(ventaRepo)
    }

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val codigo = result.data?.getStringExtra(Constants.CODIGO_SCANEADO)
                if (!codigo.isNull_or_blank_safe()) {
                    searchProductos.setQuery(codigo, true)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_punto_venta)

        initViews()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun initViews() {
        btnVolver = findViewById(R.id.btnVolver)
        searchProductos = findViewById(R.id.searchProductos)
        btnEscanear = findViewById(R.id.btnEscanear)
        recyclerProductos = findViewById(R.id.recyclerProductos)

        chipTodos = findViewById(R.id.chipTodos)
        chipAbarrotes = findViewById(R.id.chipAbarrotes)
        chipLacteos = findViewById(R.id.chipLacteos)
        chipBebidas = findViewById(R.id.chipBebidas)
        chipLimpieza = findViewById(R.id.chipLimpieza)
        chipSnacks = findViewById(R.id.chipSnacks)

        tabCarrito = findViewById(R.id.tabCarrito)
        tabInventario = findViewById(R.id.tabInventario)
        tabHistorial = findViewById(R.id.tabHistorial)
        tabProveedores = findViewById(R.id.tabProveedores)
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(emptyList()) { producto ->
            ventaViewModel.agregarAlCarrito(producto)
            Toast.makeText(this, "Añadido al carrito: ${producto.nombre}", Toast.LENGTH_SHORT).show()
        }
        recyclerProductos.layoutManager = LinearLayoutManager(this)
        recyclerProductos.adapter = productAdapter
    }

    private fun setupListeners() {
        btnVolver.setOnClickListener {
            finish()
        }

        btnEscanear.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            scannerLauncher.launch(intent)
        }

        searchProductos.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filtrarProductos()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarProductos()
                return true
            }
        })

        // Chips listener
        val chipClickListener = {
            filtrarProductos()
        }

        chipTodos.setOnClickListener { chipClickListener() }
        chipAbarrotes.setOnClickListener { chipClickListener() }
        chipLacteos.setOnClickListener { chipClickListener() }
        chipBebidas.setOnClickListener { chipClickListener() }
        chipLimpieza.setOnClickListener { chipClickListener() }
        chipSnacks.setOnClickListener { chipClickListener() }

        // Bottom nav actions
        tabCarrito.setOnClickListener {
            mostrarCarritoDialog()
        }

        tabInventario.setOnClickListener {
            val intent = Intent(this, RegistroProductoActivity::class.java)
            startActivity(intent)
        }

        tabHistorial.setOnClickListener {
            val intent = Intent(this, HistorialVentaActivity::class.java)
            startActivity(intent)
        }

        tabProveedores.setOnClickListener {
            val intent = Intent(this, ProveedorActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        productoViewModel.productos.observe(this) { list ->
            allProductsList = list
            filtrarProductos()
        }

        ventaViewModel.ventaResult.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(this, resource.data ?: "Venta realizada", Toast.LENGTH_SHORT).show()
                    ventaViewModel.resetVentaResult()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message ?: "Error en venta", Toast.LENGTH_LONG).show()
                    ventaViewModel.resetVentaResult()
                }
                else -> {}
            }
        }
    }

    private fun filtrarProductos() {
        val query = searchProductos.query.toString().trim().lowercase()

        val filtered = allProductsList.filter { prod ->
            val matchesQuery = query.isEmpty() ||
                    prod.nombre.lowercase().contains(query) ||
                    prod.codigoBarras.lowercase().contains(query)

            val matchesChip = when {
                chipTodos.isChecked -> true
                chipAbarrotes.isChecked -> prod.categoriaId == 1
                chipLacteos.isChecked -> prod.categoriaId == 2
                chipBebidas.isChecked -> prod.categoriaId == 3
                chipLimpieza.isChecked -> prod.categoriaId == 4
                chipSnacks.isChecked -> prod.categoriaId == 5
                else -> true
            }

            matchesQuery && matchesChip
        }

        productAdapter.updateProductos(filtered)
    }

    private fun mostrarCarritoDialog() {
        val intent = Intent(this, CarritoActivity::class.java)
        startActivity(intent)
    }

    private fun String?.isNull_or_blank_safe(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
