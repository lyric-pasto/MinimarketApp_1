package com.aplicaion.minimarketapp

import android.content.Intent
import android.net.Uri
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
import com.aplicaion.minimarketapp.api.JsonDatabaseManager
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Proveedor
import com.aplicaion.minimarketapp.repository.ProveedorRepository
import com.aplicaion.minimarketapp.utils.Resource
import com.aplicaion.minimarketapp.utils.SessionManager
import com.aplicaion.minimarketapp.viewmodel.ProveedorViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProveedorActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvProveedores: RecyclerView
    private lateinit var etBuscarProveedor: EditText
    private lateinit var fabAgregarProveedor: FloatingActionButton
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var adapter: ProveedorAdapter

    private var fullList: List<Proveedor> = emptyList()
    private lateinit var sessionManager: SessionManager

    private val proveedorViewModel: ProveedorViewModel by viewModels {
        val db = AppDatabase.getInstance(this)
        val repo = ProveedorRepository(db.proveedorDao())
        ProveedorViewModel.Factory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proveedor)

        sessionManager = SessionManager.getInstance(this)

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

        toolbar.title = "Proveedores"
        toolbar.subtitle = if (sessionManager.isAdmin) "Modo Administrador" else "Modo Vendedor"

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
                R.id.nav_proveedores -> true
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ProveedorAdapter(
            list = emptyList(),
            onItemClick = { proveedor ->
                if (sessionManager.isAdmin) {
                    mostrarOpcionesProveedor(proveedor)
                } else {
                    mostrarDetalleProveedor(proveedor)
                }
            },
            onCallClick = { proveedor ->
                llamarProveedor(proveedor)
            },
            onWhatsappClick = { proveedor ->
                abrirWhatsappProveedor(proveedor)
            }
        )
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

    private fun llamarProveedor(proveedor: Proveedor) {
        val tel = proveedor.celular.trim().replace(" ", "").replace("-", "")
        if (tel.isEmpty()) {
            Toast.makeText(this, "El proveedor no tiene teléfono registrado", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo iniciar la llamada: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirWhatsappProveedor(proveedor: Proveedor) {
        var tel = proveedor.celular.trim().replace(" ", "").replace("-", "").replace("+", "")
        if (tel.isEmpty()) {
            Toast.makeText(this, "El proveedor no tiene celular registrado", Toast.LENGTH_SHORT).show()
            return
        }
        // Si el número no tiene prefijo de país y es peruano (9 dígitos), añadimos 51
        if (tel.length == 9 && !tel.startsWith("51")) {
            tel = "51$tel"
        }
        val mensaje = "Hola ${proveedor.nombre}, le escribimos desde el Minimarket sobre un pedido de productos."
        val url = "https://wa.me/$tel?text=${Uri.encode(mensaje)}"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir WhatsApp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDetalleProveedor(proveedor: Proveedor) {
        AlertDialog.Builder(this)
            .setTitle(proveedor.nombre)
            .setMessage("RUC: ${proveedor.ruc}\nTeléfono: ${proveedor.celular}\nCorreo: ${proveedor.correo}\nDirección: ${proveedor.direccion}")
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun mostrarOpcionesProveedor(proveedor: Proveedor) {
        val opciones = arrayOf("Editar Proveedor", "Eliminar Proveedor", "Llamar", "Enviar WhatsApp")
        AlertDialog.Builder(this)
            .setTitle(proveedor.nombre)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> mostrarDialogoFormularioProveedor(proveedor)
                    1 -> confirmarEliminarProveedor(proveedor)
                    2 -> llamarProveedor(proveedor)
                    3 -> abrirWhatsappProveedor(proveedor)
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
            hint = "Nombre / Razón Social *"
            setText(proveedor?.nombre.orEmpty())
        }
        val etRuc = EditText(this).apply {
            hint = "RUC (11 dígitos) *"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(proveedor?.ruc.orEmpty())
        }
        val etCelular = EditText(this).apply {
            hint = "Teléfono / Celular (ej. 987654321) *"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setText(proveedor?.celular.orEmpty())
        }
        val etDireccion = EditText(this).apply {
            hint = "Dirección"
            setText(proveedor?.direccion.orEmpty())
        }
        val etCorreo = EditText(this).apply {
            hint = "Correo Electrónico"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
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
                val nombre = etNombre.text.toString().trim()
                val ruc = etRuc.text.toString().trim()
                val celular = etCelular.text.toString().trim()
                val direccion = etDireccion.text.toString().trim()
                val correo = etCorreo.text.toString().trim()

                val validacion = JsonDatabaseManager.validarProveedor(nombre, ruc, celular, correo, direccion)
                if (!validacion.isValid) {
                    Toast.makeText(this, validacion.message, Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (proveedor == null) {
                    proveedorViewModel.registrarProveedor(nombre, ruc, celular, direccion, correo)
                } else {
                    val pActualizado = proveedor.copy(
                        nombre = nombre,
                        ruc = ruc,
                        celular = celular,
                        direccion = direccion,
                        correo = correo
                    )
                    proveedorViewModel.actualizarProveedor(pActualizado)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    class ProveedorAdapter(
        private var list: List<Proveedor>,
        private val onItemClick: (Proveedor) -> Unit,
        private val onCallClick: (Proveedor) -> Unit,
        private val onWhatsappClick: (Proveedor) -> Unit
    ) : RecyclerView.Adapter<ProveedorAdapter.ProveedorViewHolder>() {

        class ProveedorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNombreProveedor: TextView = view.findViewById(R.id.tvNombreProveedor)
            val tvRuc: TextView = view.findViewById(R.id.tvRuc)
            val tvContacto: TextView = view.findViewById(R.id.tvContacto)
            val tvDireccion: TextView = view.findViewById(R.id.tvDireccion)
            val btnLlamar: MaterialButton = view.findViewById(R.id.btnLlamar)
            val btnWhatsapp: MaterialButton = view.findViewById(R.id.btnWhatsapp)
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
            holder.tvContacto.text = "Tel: ${prov.celular} • Correo: ${if (prov.correo.isNotBlank()) prov.correo else "Sin correo"}"
            holder.tvDireccion.text = "Dirección: ${if (prov.direccion.isNotBlank()) prov.direccion else "No registrada"}"

            holder.btnLlamar.setOnClickListener {
                onCallClick(prov)
            }

            holder.btnWhatsapp.setOnClickListener {
                onWhatsappClick(prov)
            }

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
