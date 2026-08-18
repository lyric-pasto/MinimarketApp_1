package com.aplicaion.minimarketapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class InventarioMainActivity : AppCompatActivity() {

    private lateinit var cardModuloProductos: MaterialCardView
    private lateinit var cardModuloCategorias: MaterialCardView
    private lateinit var cardModuloReportes: MaterialCardView
    private lateinit var tvSubtituloProductos: TextView
    private lateinit var tvSubtituloCategorias: TextView
    private lateinit var tvSubtituloReportes: TextView
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventario_main)

        sessionManager = SessionManager.getInstance(this)
        if (!sessionManager.isAdmin) {
            Toast.makeText(this, "Acceso restringido a Administradores", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupListeners()
        loadStats()
    }

    private fun initViews() {
        cardModuloProductos = findViewById(R.id.cardModuloProductos)
        cardModuloCategorias = findViewById(R.id.cardModuloCategorias)
        cardModuloReportes = findViewById(R.id.cardModuloReportes)
        tvSubtituloProductos = findViewById(R.id.tvSubtituloProductos)
        tvSubtituloCategorias = findViewById(R.id.tvSubtituloCategorias)
        tvSubtituloReportes = findViewById(R.id.tvSubtituloReportes)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        bottomNavigation.selectedItemId = R.id.nav_inventario
    }

    private fun setupListeners() {
        // MÓDULO 1: PRODUCTOS (Catálogo, edición y registro)
        cardModuloProductos.setOnClickListener {
            val intent = Intent(this, GestionProductosListActivity::class.java)
            startActivity(intent)
        }

        // MÓDULO 2: CATEGORÍAS (Estructura de carpetas)
        cardModuloCategorias.setOnClickListener {
            val intent = Intent(this, CategoriasActivity::class.java)
            startActivity(intent)
        }

        // MÓDULO 3: REPORTES (Reportes, exportación y anulación)
        cardModuloReportes.setOnClickListener {
            val intent = Intent(this, HistorialVentaActivity::class.java)
            startActivity(intent)
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
                R.id.nav_inventario -> true
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

    private fun loadStats() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            db.productoDao().getAll().collect { prods ->
                tvSubtituloProductos.text = "${prods.size} productos registrados en catálogo"
            }
        }
        lifecycleScope.launch {
            db.categoriaDao().getAll().collect { cats ->
                tvSubtituloCategorias.text = "${cats.size} carpetas de categorías activas"
            }
        }
        lifecycleScope.launch {
            db.ventaDao().getAll().collect { ventas ->
                tvSubtituloReportes.text = "${ventas.size} ventas registradas para reportes"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bottomNavigation.selectedItemId = R.id.nav_inventario
    }
}
