package com.aplicaion.minimarketapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.api.JsonDatabaseManager
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Categoria
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CategoriasActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnAgregarCategoria: ImageButton
    private lateinit var tvTotalCategoriasCount: TextView
    private lateinit var rvCategorias: RecyclerView

    private lateinit var adapter: CategoriaCarpetaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categorias)

        initViews()
        setupRecyclerView()
        loadData()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbarCategorias)
        btnAgregarCategoria = findViewById(R.id.btnAgregarCategoria)
        tvTotalCategoriasCount = findViewById(R.id.tvTotalCategoriasCount)
        rvCategorias = findViewById(R.id.rvCategoriasCarpetas)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        btnAgregarCategoria.setOnClickListener {
            mostrarDialogCrearCategoria()
        }
    }

    private fun setupRecyclerView() {
        adapter = CategoriaCarpetaAdapter(
            items = emptyList(),
            onCarpetaClick = { cat ->
                val intent = Intent(this, ProductosPorCategoriaActivity::class.java).apply {
                    putExtra("categoria_id", cat.id)
                    putExtra("categoria_nombre", cat.nombre)
                }
                startActivity(intent)
            },
            onEditarClick = { cat ->
                mostrarOpcionesCategoria(cat)
            }
        )
        rvCategorias.layoutManager = LinearLayoutManager(this)
        rvCategorias.adapter = adapter
    }

    private fun loadData() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val categoriasFlow = db.categoriaDao().getAll()
            val productosFlow = db.productoDao().getAll()

            categoriasFlow.combine(productosFlow) { categorias, productos ->
                categorias.map { cat ->
                    val conteo = productos.count { it.categoriaId == cat.id }
                    CategoriaConConteo(cat, conteo)
                }
            }.collect { listaConConteo ->
                adapter.updateData(listaConConteo)
                tvTotalCategoriasCount.text = "${listaConConteo.size} categorías"
            }
        }
    }

    private fun mostrarDialogCrearCategoria() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_crear_categoria, null)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreCategoria)
        val etDescripcion = dialogView.findViewById<TextInputEditText>(R.id.etDescripcionCategoria)

        AlertDialog.Builder(this)
            .setTitle("Nueva Categoría (Carpeta)")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val desc = etDescripcion.text.toString().trim()
                if (nombre.length < 2) {
                    Toast.makeText(this, "El nombre debe tener al menos 2 caracteres", Toast.LENGTH_SHORT).show()
                } else {
                    lifecycleScope.launch {
                        val db = AppDatabase.getInstance(this@CategoriasActivity)
                        val id = db.categoriaDao().insert(Categoria(nombre = nombre, descripcion = desc))
                        if (id > 0) {
                            JsonDatabaseManager.syncAndSaveJsonFile(this@CategoriasActivity, db)
                            Toast.makeText(this@CategoriasActivity, "✓ Categoría creada exitosamente", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarOpcionesCategoria(cat: Categoria) {
        val opciones = arrayOf("📁 Ver productos de esta carpeta", "✏️ Editar nombre y descripción", "🗑️ Eliminar categoría")
        AlertDialog.Builder(this)
            .setTitle("Categoría: ${cat.nombre}")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, ProductosPorCategoriaActivity::class.java).apply {
                            putExtra("categoria_id", cat.id)
                            putExtra("categoria_nombre", cat.nombre)
                        }
                        startActivity(intent)
                    }
                    1 -> mostrarDialogEditarCategoria(cat)
                    2 -> confirmarEliminarCategoria(cat)
                }
            }
            .show()
    }

    private fun mostrarDialogEditarCategoria(cat: Categoria) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_crear_categoria, null)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etNombreCategoria)
        val etDescripcion = dialogView.findViewById<TextInputEditText>(R.id.etDescripcionCategoria)

        etNombre.setText(cat.nombre)
        etDescripcion.setText(cat.descripcion)

        AlertDialog.Builder(this)
            .setTitle("Editar Categoría")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val desc = etDescripcion.text.toString().trim()
                if (nombre.length < 2) {
                    Toast.makeText(this, "El nombre debe tener al menos 2 caracteres", Toast.LENGTH_SHORT).show()
                } else {
                    lifecycleScope.launch {
                        val db = AppDatabase.getInstance(this@CategoriasActivity)
                        db.categoriaDao().update(cat.copy(nombre = nombre, descripcion = desc))
                        JsonDatabaseManager.syncAndSaveJsonFile(this@CategoriasActivity, db)
                        Toast.makeText(this@CategoriasActivity, "✓ Categoría actualizada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminarCategoria(cat: Categoria) {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val count = db.productoDao().getCountByCategoria(cat.id)
            if (count > 0) {
                AlertDialog.Builder(this@CategoriasActivity)
                    .setTitle("No se puede eliminar")
                    .setMessage("La categoría '${cat.nombre}' contiene $count producto(s). Reasigna o elimina los productos antes de borrar la categoría.")
                    .setPositiveButton("Entendido", null)
                    .show()
            } else {
                AlertDialog.Builder(this@CategoriasActivity)
                    .setTitle("Eliminar Categoría")
                    .setMessage("¿Estás seguro de eliminar la carpeta de categoría '${cat.nombre}'?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        lifecycleScope.launch {
                            db.categoriaDao().delete(cat)
                            JsonDatabaseManager.syncAndSaveJsonFile(this@CategoriasActivity, db)
                            Toast.makeText(this@CategoriasActivity, "Categoría eliminada", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }
}
