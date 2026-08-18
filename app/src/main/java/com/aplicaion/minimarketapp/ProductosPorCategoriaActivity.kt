package com.aplicaion.minimarketapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.db.AppDatabase
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ProductosPorCategoriaActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvNombreCarpetaHeader: TextView
    private lateinit var tvConteoCarpetaHeader: TextView
    private lateinit var btnNuevoProductoEnCarpeta: MaterialButton
    private lateinit var rvProductos: RecyclerView

    private lateinit var adapter: ProductosArchivoAdapter
    private var categoriaId: Int = 1
    private var categoriaNombre: String = "Categoría"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_productos_por_categoria)

        categoriaId = intent.getIntExtra("categoria_id", 1)
        categoriaNombre = intent.getStringExtra("categoria_nombre") ?: "Categoría"

        initViews()
        setupRecyclerView()
        loadData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbarProductosCategoria)
        tvNombreCarpetaHeader = findViewById(R.id.tvNombreCarpetaHeader)
        tvConteoCarpetaHeader = findViewById(R.id.tvConteoCarpetaHeader)
        btnNuevoProductoEnCarpeta = findViewById(R.id.btnNuevoProductoEnCarpeta)
        rvProductos = findViewById(R.id.rvProductosEnCarpeta)

        toolbar.title = "Carpeta: $categoriaNombre"
        toolbar.setNavigationOnClickListener { finish() }

        tvNombreCarpetaHeader.text = categoriaNombre

        btnNuevoProductoEnCarpeta.setOnClickListener {
            val intent = Intent(this, RegistroProductoActivity::class.java).apply {
                putExtra("preselected_categoria_id", categoriaId)
            }
            startActivity(intent)
        }
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

    private fun loadData() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            db.productoDao().getByCategoria(categoriaId).collect { list ->
                adapter.updateData(list)
                tvConteoCarpetaHeader.text = "${list.size} archivos de productos registrados"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }
}
