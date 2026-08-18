package com.aplicaion.minimarketapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Producto
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.launch

class GestionProductosListActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var searchView: SearchView
    private lateinit var tvConteo: TextView
    private lateinit var rvProductos: RecyclerView
    private lateinit var fabNuevo: ExtendedFloatingActionButton

    private lateinit var adapter: ProductosArchivoAdapter
    private var allProducts: List<Producto> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_productos_list)

        initViews()
        setupRecyclerView()
        setupListeners()
        loadData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbarGestionProductos)
        searchView = findViewById(R.id.searchGestionProductos)
        tvConteo = findViewById(R.id.tvConteoProductosTotal)
        rvProductos = findViewById(R.id.rvGestionProductos)
        fabNuevo = findViewById(R.id.fabNuevoProducto)

        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = ProductosArchivoAdapter(emptyList()) { prod ->
            val intent = Intent(this, RegistroProductoActivity::class.java).apply {
                putExtra("producto_id", prod.id)
            }
            startActivity(intent)
        }
        rvProductos.layoutManager = LinearLayoutManager(this)
        rvProductos.adapter = adapter
    }

    private fun setupListeners() {
        fabNuevo.setOnClickListener {
            val intent = Intent(this, RegistroProductoActivity::class.java)
            startActivity(intent)
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filtrar(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrar(newText)
                return true
            }
        })
    }

    private fun loadData() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            db.productoDao().getAll().collect { list ->
                allProducts = list
                filtrar(searchView.query?.toString())
            }
        }
    }

    private fun filtrar(query: String?) {
        val q = query?.trim()?.lowercase() ?: ""
        val filtrados = if (q.isEmpty()) {
            allProducts
        } else {
            allProducts.filter {
                it.nombre.lowercase().contains(q) ||
                it.codigoBarras.lowercase().contains(q) ||
                it.descripcion.lowercase().contains(q)
            }
        }
        tvConteo.text = "Mostrando ${filtrados.size} de ${allProducts.size} productos"
        adapter.updateData(filtrados)
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }
}
