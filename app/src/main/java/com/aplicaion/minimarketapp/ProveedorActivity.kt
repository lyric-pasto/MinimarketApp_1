package com.aplicaion.minimarketapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Proveedor
import com.aplicaion.minimarketapp.repository.ProveedorRepository
import com.aplicaion.minimarketapp.utils.Resource
import com.aplicaion.minimarketapp.viewmodel.ProveedorViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProveedorActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvProveedores: RecyclerView
    private lateinit var etBuscarProveedor: EditText
    private lateinit var fabAgregarProveedor: FloatingActionButton
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var adapter: ProveedorAdapter

    private var fullList: List<Proveedor> = emptyList()

    private val proveedorViewModel: ProveedorViewModel by viewModels {
        val db = AppDatabase.getInstance(this)
        val repo = ProveedorRepository(db.proveedorDao())
        ProveedorViewModel.Factory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proveedor)

        initViews()
        setupListeners()
        setupRecyclerView()
        observeViewModel()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvProveedores = findViewById(R.id.rvProveedores)
        etBuscarProveedor = findViewById(R.id.etBuscarProveedor)
        fabAgregarProveedor = findViewById(R.id.fabAgregarProveedor)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        bottomNavigation.selectedItemId = R.id.nav_proveedores
    }

    private fun setupListeners() {
        etBuscarProveedor.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarProveedores(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        fabAgregarProveedor.setOnClickListener {
            mostrarDialogoFormularioProveedor(null)
        }

        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_pos -> {
                    val intent = Intent(this, punto_venta::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_carrito -> {
                    val intent = Intent(this, CarritoActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_inventario -> {
                    val intent = Intent(this, RegistroProductoActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_historial -> {
                    val intent = Intent(this, HistorialVentaActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_proveedores -> true
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ProveedorAdapter(emptyList()) { proveedor ->
            mostrarOpcionesProveedor(proveedor)
        }
        rvProveedores.layoutManager = LinearLayoutManager(this)
        rvProveedores.adapter = adapter
    }

    private fun observeViewModel() {
        proveedorViewModel.proveedores.observe(this) { list ->
            fullList = list
            filtrarProveedores(etBuscarProveedor.text.toString())
        }

        proveedorViewModel.guardarState.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(this, resource.data, Toast.LENGTH_SHORT).show()
                    proveedorViewModel.resetState()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                    proveedorViewModel.resetState()
                }
                else -> {}
            }
        }
    }

    private fun filtrarProveedores(query: String) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            adapter.updateProveedores(fullList)
        } else {
            val filtered = fullList.filter {
                it.nombre.lowercase().contains(q) || it.ruc.lowercase().contains(q) || it.celular.contains(q)
            }
            adapter.updateProveedores(filtered)
        }
    }

    private fun mostrarOpcionesProveedor(proveedor: Proveedor) {
        val opciones = arrayOf("Editar", "Eliminar")
        AlertDialog.Builder(this)
            .setTitle(proveedor.nombre)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> mostrarDialogoFormularioProveedor(proveedor)
                    1 -> confirmarEliminarProveedor(proveedor)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminarProveedor(proveedor: Proveedor) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Proveedor")
            .setMessage("¿Estás seguro de eliminar a ${proveedor.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                proveedorViewModel.eliminarProveedor(proveedor)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoFormularioProveedor(proveedor: Proveedor?) {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etNombre = EditText(this).apply {
            hint = "Nombre del Proveedor"
            setText(proveedor?.nombre.orEmpty())
        }
        val etRuc = EditText(this).apply {
            hint = "RUC"
            setText(proveedor?.ruc.orEmpty())
        }
        val etCelular = EditText(this).apply {
            hint = "Celular"
            setText(proveedor?.celular.orEmpty())
        }
        val etDireccion = EditText(this).apply {
            hint = "Dirección"
            setText(proveedor?.direccion.orEmpty())
        }
        val etCorreo = EditText(this).apply {
            hint = "Correo Electrónico"
            setText(proveedor?.correo.orEmpty())
        }

        layout.addView(etNombre)
        layout.addView(etRuc)
        layout.addView(etCelular)
        layout.addView(etDireccion)
        layout.addView(etCorreo)

        val titulo = if (proveedor == null) "Nuevo Proveedor" else "Editar Proveedor"

        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = etNombre.text.toString()
                val ruc = etRuc.text.toString()
                val celular = etCelular.text.toString()
                val direccion = etDireccion.text.toString()
                val correo = etCorreo.text.toString()

                if (proveedor == null) {
                    proveedorViewModel.registrarProveedor(nombre, ruc, celular, direccion, correo)
                } else {
                    val pActualizado = proveedor.copy(
                        nombre = nombre.trim(),
                        ruc = ruc.trim(),
                        celular = celular.trim(),
                        direccion = direccion.trim(),
                        correo = correo.trim()
                    )
                    proveedorViewModel.actualizarProveedor(pActualizado)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    class ProveedorAdapter(
        private var list: List<Proveedor>,
        private val onItemClick: (Proveedor) -> Unit
    ) : RecyclerView.Adapter<ProveedorAdapter.ProveedorViewHolder>() {

        class ProveedorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNombreProveedor: TextView = view.findViewById(R.id.tvNombreProveedor)
            val tvRuc: TextView = view.findViewById(R.id.tvRuc)
            val tvContacto: TextView = view.findViewById(R.id.tvContacto)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProveedorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_proveedor, parent, false)
            return ProveedorViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProveedorViewHolder, position: Int) {
            val prov = list[position]
            holder.tvNombreProveedor.text = prov.nombre
            holder.tvRuc.text = "RUC: ${prov.ruc}"
            holder.tvContacto.text = "Cel: ${prov.celular} - Correo: ${prov.correo}"
            holder.itemView.setOnClickListener {
                onItemClick(prov)
            }
        }

        override fun getItemCount(): Int = list.size

        fun updateProveedores(newList: List<Proveedor>) {
            list = newList
            notifyDataSetChanged()
        }
    }
}
