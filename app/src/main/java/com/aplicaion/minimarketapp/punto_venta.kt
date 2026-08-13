package com.aplicaion.minimarketapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.api.MinimarketApiProvider
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.repository.CategoriaRepository
import com.aplicaion.minimarketapp.repository.ProductoRepository
import com.aplicaion.minimarketapp.repository.VentaRepository
import com.aplicaion.minimarketapp.utils.Constants
import com.aplicaion.minimarketapp.utils.SessionManager
import com.aplicaion.minimarketapp.viewmodel.CarritoViewModel
import com.aplicaion.minimarketapp.viewmodel.ProductoViewModel
import com.aplicaion.minimarketapp.viewmodel.VentaViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class punto_venta : AppCompatActivity() {

    private lateinit var tvNombreUsuarioHeader: TextView
    private lateinit var tvRolUsuarioHeader: TextView
    private lateinit var btnGestionUsuarios: ImageButton
    private lateinit var tvBadgeUsuarios: TextView
    private lateinit var btnCerrarSesion: ImageButton

    private lateinit var searchProductos: SearchView
    private lateinit var btnEscanear: ImageButton
    private lateinit var recyclerProductos: RecyclerView
    private lateinit var tvCantidadProductos: TextView

    private lateinit var chipGroupCategorias: ChipGroup
    private lateinit var chipTodos: Chip
    private lateinit var chipAbarrotes: Chip
    private lateinit var chipLacteos: Chip
    private lateinit var chipBebidas: Chip
    private lateinit var chipLimpieza: Chip
    private lateinit var chipSnacks: Chip
    private lateinit var chipCuidadoPersonal: Chip

    private lateinit var bottomNavigation: com.google.android.material.bottomnavigation.BottomNavigationView

    private lateinit var productAdapter: ProductAdapter
    private var allProductsList: List<Producto> = emptyList()

    private val carritoViewModel = CarritoViewModel.getInstance()
    private val api by lazy { MinimarketApiProvider.getApi(this) }
    private lateinit var sessionManager: SessionManager

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
                if (!codigo.isNullOrBlank()) {
                    searchProductos.setQuery(codigo, true)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_punto_venta)

        sessionManager = SessionManager.getInstance(this)

        initViews()
        setupUserHeader()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun initViews() {
        tvNombreUsuarioHeader = findViewById(R.id.tvNombreUsuarioHeader)
        tvRolUsuarioHeader = findViewById(R.id.tvRolUsuarioHeader)
        btnGestionUsuarios = findViewById(R.id.btnGestionUsuarios)
        tvBadgeUsuarios = findViewById(R.id.tvBadgeUsuarios)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)

        searchProductos = findViewById(R.id.searchProductos)
        btnEscanear = findViewById(R.id.btnEscanear)
        recyclerProductos = findViewById(R.id.recyclerProductos)
        tvCantidadProductos = findViewById(R.id.tvCantidadProductos)

        chipGroupCategorias = findViewById(R.id.chipGroupCategorias)
        chipTodos = findViewById(R.id.chipTodos)
        chipAbarrotes = findViewById(R.id.chipAbarrotes)
        chipLacteos = findViewById(R.id.chipLacteos)
        chipBebidas = findViewById(R.id.chipBebidas)
        chipLimpieza = findViewById(R.id.chipLimpieza)
        chipSnacks = findViewById(R.id.chipSnacks)
        chipCuidadoPersonal = findViewById(R.id.chipCuidadoPersonal)

        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_pos
    }

    private fun setupUserHeader() {
        val userName = sessionManager.userName.ifEmpty { "Minimarket POS" }
        val rol = sessionManager.userRole

        tvNombreUsuarioHeader.text = userName

        if (sessionManager.isAdmin) {
            tvRolUsuarioHeader.text = "ADMINISTRADOR"
            tvRolUsuarioHeader.setTextColor(ContextCompat.getColor(this, R.color.morado_scanner))
            btnGestionUsuarios.visibility = View.VISIBLE
        } else {
            tvRolUsuarioHeader.text = "VENDEDOR"
            tvRolUsuarioHeader.setTextColor(ContextCompat.getColor(this, R.color.verde_ganancia))
            btnGestionUsuarios.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(emptyList()) { producto ->
            val exito = carritoViewModel.agregar(producto)
            if (exito) {
                Toast.makeText(this, "✓ ${producto.nombre} añadido al carrito", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Stock máximo alcanzado para ${producto.nombre}", Toast.LENGTH_SHORT).show()
            }
        }
        val layoutManager = LinearLayoutManager(this)
        recyclerProductos.layoutManager = layoutManager
        recyclerProductos.setHasFixedSize(true)
        recyclerProductos.adapter = productAdapter
    }

    private fun setupListeners() {
        btnCerrarSesion.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Deseas cerrar tu sesión actual?")
                .setPositiveButton("Salir") { _, _ ->
                    sessionManager.clearSession()
                    val intent = Intent(this, inicio_sesion::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnGestionUsuarios.setOnClickListener {
            if (sessionManager.isAdmin) {
                val intent = Intent(this, GestionUsuariosActivity::class.java)
                startActivity(intent)
            }
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

        chipGroupCategorias.setOnCheckedStateChangeListener { _, _ ->
            filtrarProductos()
        }

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_pos -> true
                R.id.nav_carrito -> {
                    val intent = Intent(this, CarritoActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_inventario -> {
                    if (sessionManager.isAdmin) {
                        val intent = Intent(this, RegistroProductoActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                    } else {
                        Toast.makeText(
                            this,
                            "Acceso restringido: Solo el Administrador puede gestionar o añadir productos al inventario",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    true
                }
                R.id.nav_historial -> {
                    val intent = Intent(this, HistorialVentaActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_proveedores -> {
                    val intent = Intent(this, ProveedorActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        productoViewModel.productos.observe(this) { list ->
            allProductsList = list
            filtrarProductos()
        }

        lifecycleScope.launch {
            carritoViewModel.totalItems.collect { total ->
                val badge = bottomNavigation.getOrCreateBadge(R.id.nav_carrito)
                if (total > 0) {
                    badge.isVisible = true
                    badge.number = total
                    badge.backgroundColor = ContextCompat.getColor(this@punto_venta, R.color.amarillo_accion)
                    badge.badgeTextColor = ContextCompat.getColor(this@punto_venta, R.color.texto_sobre_amarillo)
                } else {
                    badge.isVisible = false
                }
            }
        }

        // Monitorear solicitudes de usuarios pendientes para Administradores
        if (sessionManager.isAdmin) {
            lifecycleScope.launch {
                val api = MinimarketApiProvider.getApi(this@punto_venta)
                api.getUsuariosFlow().collect { list ->
                    val pendientes = list.count { it.estado.equals(Constants.ESTADO_PENDIENTE, ignoreCase = true) }
                    if (pendientes > 0) {
                        tvBadgeUsuarios.visibility = View.VISIBLE
                        tvBadgeUsuarios.text = pendientes.toString()
                    } else {
                        tvBadgeUsuarios.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.nav_pos
    }

    private fun filtrarProductos() {
        val query = searchProductos.query.toString().trim().lowercase()
        val checkedChipId = chipGroupCategorias.checkedChipId

        val filtered = allProductsList.filter { prod ->
            val matchesQuery = query.isEmpty() ||
                    prod.nombre.lowercase().contains(query) ||
                    prod.codigoBarras.lowercase().contains(query)

            val matchesCategory = when (checkedChipId) {
                R.id.chipAbarrotes -> prod.categoriaId == 1
                R.id.chipLacteos -> prod.categoriaId == 2
                R.id.chipBebidas -> prod.categoriaId == 3
                R.id.chipLimpieza -> prod.categoriaId == 4
                R.id.chipSnacks -> prod.categoriaId == 5
                R.id.chipCuidadoPersonal -> prod.categoriaId == 6
                else -> true
            }

            matchesQuery && matchesCategory
        }

        tvCantidadProductos.text = "${filtered.size} producto(s) encontrado(s)"
        productAdapter.updateProductos(filtered)
    }

    private fun mostrarCarritoDialog() {
        val intent = Intent(this, CarritoActivity::class.java)
        startActivity(intent)
    }
}
