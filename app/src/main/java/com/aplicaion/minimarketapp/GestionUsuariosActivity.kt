package com.aplicaion.minimarketapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aplicaion.minimarketapp.api.ApiResponse
import com.aplicaion.minimarketapp.api.JsonDatabaseManager
import com.aplicaion.minimarketapp.api.MinimarketApiProvider
import com.aplicaion.minimarketapp.db.entity.Usuario
import com.aplicaion.minimarketapp.utils.Constants
import com.aplicaion.minimarketapp.utils.SessionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GestionUsuariosActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var cardNotificacionSolicitudes: MaterialCardView
    private lateinit var tvTituloNotificacion: TextView
    private lateinit var tvSubtituloNotificacion: TextView
    private lateinit var btnVerSolicitudes: MaterialButton
    private lateinit var etBuscarUsuario: EditText
    private lateinit var chipGroupFiltros: ChipGroup
    private lateinit var chipTodos: Chip
    private lateinit var chipSolicitudes: Chip
    private lateinit var chipActivos: Chip
    private lateinit var chipInactivos: Chip
    private lateinit var chipAdmins: Chip
    private lateinit var chipVendedores: Chip
    private lateinit var tvTotalUsuarios: TextView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmptyTitle: TextView
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var rvUsuarios: RecyclerView
    private lateinit var fabAgregarUsuario: ExtendedFloatingActionButton

    private lateinit var adapter: UsuarioAdapter
    private var fullUsersList: List<Usuario> = emptyList()
    private var currentFilterMode: FilterMode = FilterMode.TODOS

    enum class FilterMode {
        TODOS, SOLICITUDES, ACTIVOS, INACTIVOS, ADMINS, VENDEDORES
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager.getInstance(this)
        if (!sessionManager.isAdmin) {
            Toast.makeText(
                this,
                "Acceso restringido: Solo el Administrador puede gestionar usuarios",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        setContentView(R.layout.activity_gestion_usuarios)

        initViews()
        setupToolbarMenu()
        setupListeners()
        setupRecyclerView()
        observeUsers()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        cardNotificacionSolicitudes = findViewById(R.id.cardNotificacionSolicitudes)
        tvTituloNotificacion = findViewById(R.id.tvTituloNotificacion)
        tvSubtituloNotificacion = findViewById(R.id.tvSubtituloNotificacion)
        btnVerSolicitudes = findViewById(R.id.btnVerSolicitudes)
        etBuscarUsuario = findViewById(R.id.etBuscarUsuario)
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros)
        chipTodos = findViewById(R.id.chipTodos)
        chipSolicitudes = findViewById(R.id.chipSolicitudes)
        chipActivos = findViewById(R.id.chipActivos)
        chipInactivos = findViewById(R.id.chipInactivos)
        chipAdmins = findViewById(R.id.chipAdmins)
        chipVendedores = findViewById(R.id.chipVendedores)
        tvTotalUsuarios = findViewById(R.id.tvTotalUsuarios)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle)
        rvUsuarios = findViewById(R.id.rvUsuarios)
        fabAgregarUsuario = findViewById(R.id.fabAgregarUsuario)

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupToolbarMenu() {
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_nuevo_usuario -> {
                    mostrarDialogoRegistrarDirecto()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupListeners() {
        etBuscarUsuario.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                aplicarFiltros()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        chipGroupFiltros.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilterMode = when {
                checkedIds.contains(R.id.chipSolicitudes) -> FilterMode.SOLICITUDES
                checkedIds.contains(R.id.chipActivos) -> FilterMode.ACTIVOS
                checkedIds.contains(R.id.chipInactivos) -> FilterMode.INACTIVOS
                checkedIds.contains(R.id.chipAdmins) -> FilterMode.ADMINS
                checkedIds.contains(R.id.chipVendedores) -> FilterMode.VENDEDORES
                else -> FilterMode.TODOS
            }
            aplicarFiltros()
        }

        btnVerSolicitudes.setOnClickListener {
            chipSolicitudes.isChecked = true
        }

        fabAgregarUsuario.setOnClickListener {
            mostrarDialogoRegistrarDirecto()
        }
    }

    private fun setupRecyclerView() {
        adapter = UsuarioAdapter(
            onAceptar = { usuario -> aceptarSolicitud(usuario) },
            onRechazar = { usuario -> rechazarSolicitud(usuario) },
            onAsignarRol = { usuario -> asignarRol(usuario) },
            onToggleEstado = { usuario -> toggleEstado(usuario) },
            onCambiarRol = { usuario -> cambiarRol(usuario) },
            onMasOpciones = { usuario -> mostrarOpcionesUsuario(usuario) }
        )
        rvUsuarios.layoutManager = LinearLayoutManager(this)
        rvUsuarios.setHasFixedSize(true)
        rvUsuarios.adapter = adapter
    }

    private fun observeUsers() {
        lifecycleScope.launch {
            val api = MinimarketApiProvider.getApi(this@GestionUsuariosActivity)
            api.getUsuariosFlow().collectLatest { list ->
                fullUsersList = list
                actualizarBannerYSolicitudes()
                aplicarFiltros()
            }
        }
    }

    private fun actualizarBannerYSolicitudes() {
        val solicitudes = fullUsersList.filter { it.estado.equals(Constants.ESTADO_PENDIENTE, ignoreCase = true) }
        val count = solicitudes.size

        chipSolicitudes.text = "🔔 Solicitudes ($count)"

        if (count > 0) {
            cardNotificacionSolicitudes.visibility = View.VISIBLE
            tvTituloNotificacion.text = "¡$count Nueva(s) Solicitud(es) de Acceso!"
            tvSubtituloNotificacion.text = "Hay $count usuario(s) esperando tu aprobación para ingresar."
        } else {
            cardNotificacionSolicitudes.visibility = View.GONE
        }
    }

    private fun aplicarFiltros() {
        val query = etBuscarUsuario.text.toString().trim().lowercase()

        val listByMode = when (currentFilterMode) {
            FilterMode.TODOS -> fullUsersList
            FilterMode.SOLICITUDES -> fullUsersList.filter { it.estado.equals(Constants.ESTADO_PENDIENTE, ignoreCase = true) }
            FilterMode.ACTIVOS -> fullUsersList.filter { it.estado.equals(Constants.ESTADO_ACTIVO, ignoreCase = true) }
            FilterMode.INACTIVOS -> fullUsersList.filter { it.estado.equals(Constants.ESTADO_INACTIVO, ignoreCase = true) }
            FilterMode.ADMINS -> fullUsersList.filter { it.rol.equals(Constants.ROL_ADMIN, ignoreCase = true) }
            FilterMode.VENDEDORES -> fullUsersList.filter { it.rol.equals(Constants.ROL_VENDEDOR, ignoreCase = true) }
        }

        val filtered = if (query.isEmpty()) {
            listByMode
        } else {
            listByMode.filter {
                it.nombreCompleto.lowercase().contains(query) ||
                        it.usuario.lowercase().contains(query) ||
                        it.correo.lowercase().contains(query) ||
                        it.rol.lowercase().contains(query)
            }
        }

        adapter.submitList(filtered)
        tvTotalUsuarios.text = "Mostrando ${filtered.size} de ${fullUsersList.size} usuarios"

        if (filtered.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            when (currentFilterMode) {
                FilterMode.SOLICITUDES -> {
                    tvEmptyTitle.text = "No hay solicitudes pendientes"
                    tvEmptySubtitle.text = "Todas las solicitudes de registro han sido procesadas."
                }
                FilterMode.INACTIVOS -> {
                    tvEmptyTitle.text = "No hay usuarios inhabilitados"
                    tvEmptySubtitle.text = "Todos los usuarios registrados están activos."
                }
                else -> {
                    tvEmptyTitle.text = "No se encontraron usuarios"
                    tvEmptySubtitle.text = "Prueba con otro término de búsqueda o cambia de filtro."
                }
            }
        } else {
            layoutEmpty.visibility = View.GONE
        }
    }

    // --- ACCIONES DIRECTAS ---

    private fun aceptarSolicitud(usuario: Usuario) {
        lifecycleScope.launch {
            val api = MinimarketApiProvider.getApi(this@GestionUsuariosActivity)
            val usuarioAprobado = usuario.copy(estado = Constants.ESTADO_ACTIVO)
            val res = api.actualizarUsuario(usuarioAprobado)
            if (res is ApiResponse.Success) {
                Toast.makeText(
                    this@GestionUsuariosActivity,
                    "✓ Solicitud de '${usuario.nombreCompleto}' aceptada y activada como ${usuario.rol}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this@GestionUsuariosActivity, (res as ApiResponse.Error).message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun rechazarSolicitud(usuario: Usuario) {
        AlertDialog.Builder(this)
            .setTitle("Rechazar Solicitud")
            .setMessage("¿Deseas rechazar la solicitud de acceso de ${usuario.nombreCompleto} (@${usuario.usuario})?")
            .setPositiveButton("Rechazar y Eliminar") { _, _ ->
                lifecycleScope.launch {
                    val api = MinimarketApiProvider.getApi(this@GestionUsuariosActivity)
                    val res = api.eliminarUsuario(usuario)
                    if (res is ApiResponse.Success) {
                        Toast.makeText(this@GestionUsuariosActivity, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toggleEstado(usuario: Usuario) {
        if (usuario.usuario.equals("admin", ignoreCase = true)) {
            Toast.makeText(this, "El administrador principal siempre debe estar activo", Toast.LENGTH_SHORT).show()
            return
        }

        val nuevoEstado = if (usuario.estado.equals(Constants.ESTADO_ACTIVO, ignoreCase = true)) {
            Constants.ESTADO_INACTIVO
        } else {
            Constants.ESTADO_ACTIVO
        }

        val mensaje = if (nuevoEstado == Constants.ESTADO_INACTIVO) {
            "¿Deseas inhabilitar al usuario '${usuario.usuario}'? No podrá iniciar sesión."
        } else {
            "¿Deseas habilitar al usuario '${usuario.usuario}'?"
        }

        AlertDialog.Builder(this)
            .setTitle(if (nuevoEstado == Constants.ESTADO_INACTIVO) "Inhabilitar Usuario" else "Habilitar Usuario")
            .setMessage(mensaje)
            .setPositiveButton(if (nuevoEstado == Constants.ESTADO_INACTIVO) "Inhabilitar" else "Habilitar") { _, _ ->
                lifecycleScope.launch {
                    val api = MinimarketApiProvider.getApi(this@GestionUsuariosActivity)
                    val res = api.actualizarUsuario(usuario.copy(estado = nuevoEstado))
                    if (res is ApiResponse.Success) {
                        Toast.makeText(
                            this@GestionUsuariosActivity,
                            "Usuario '${usuario.usuario}' ahora está $nuevoEstado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cambiarRol(usuario: Usuario) {
        val nuevoRol = if (usuario.rol.equals(Constants.ROL_ADMIN, ignoreCase = true)) {
            Constants.ROL_VENDEDOR
        } else {
            Constants.ROL_ADMIN
        }

        AlertDialog.Builder(this)
            .setTitle("Cambiar Rol")
            .setMessage("¿Deseas cambiar el rol de '${usuario.usuario}' de ${usuario.rol} a $nuevoRol?")
            .setPositiveButton("Sí, Cambiar a $nuevoRol") { _, _ ->
                lifecycleScope.launch {
                    val api = MinimarketApiProvider.getApi(this@GestionUsuariosActivity)
                    val res = api.actualizarUsuario(usuario.copy(rol = nuevoRol))
                    if (res is ApiResponse.Success) {
                        Toast.makeText(this@GestionUsuariosActivity, "Rol actualizado a $nuevoRol", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun asignarRol(usuario: Usuario) {
        val roles = arrayOf("VENDEDOR", "ADMIN")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar Rol para ${usuario.nombreCompleto}")
            .setItems(roles) { _, which ->
                val rolElegido = roles[which]
                lifecycleScope.launch {
                    val api = MinimarketApiProvider.getApi(this@GestionUsuariosActivity)
                    val res = api.actualizarUsuario(usuario.copy(rol = rolElegido))
                    if (res is ApiResponse.Success) {
                        Toast.makeText(this@GestionUsuariosActivity, "Rol asignado: $rolElegido", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarOpcionesUsuario(usuario: Usuario) {
        val opciones = arrayOf(
            "Cambiar Contraseña",
            "Editar Rol (${usuario.rol})",
            "Eliminar Usuario Definitivamente"
        )

        AlertDialog.Builder(this)
            .setTitle(usuario.nombreCompleto)
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> mostrarDialogoCambiarPass(usuario)
                    1 -> cambiarRol(usuario)
                    2 -> confirmarEliminarUsuario(usuario)
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun mostrarDialogoCambiarPass(usuario: Usuario) {
        val etPass = EditText(this).apply {
            hint = "Nueva Contraseña (mín. 6 caracteres)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
            addView(etPass)
        }

        AlertDialog.Builder(this)
            .setTitle("Cambiar Contraseña para @${usuario.usuario}")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val pass = etPass.text.toString().trim()
                if (pass.length < 6) {
                    Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val api = MinimarketApiProvider.getApi(this@GestionUsuariosActivity)
                    val res = api.actualizarUsuario(usuario.copy(contrasena = pass))
                    if (res is ApiResponse.Success) {
                        Toast.makeText(this@GestionUsuariosActivity, "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminarUsuario(usuario: Usuario) {
        if (usuario.usuario.equals("admin", ignoreCase = true)) {
            Toast.makeText(this, "No se puede eliminar el usuario administrador principal", Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar Usuario")
            .setMessage("¿Estás seguro de eliminar permanentemente a ${usuario.nombreCompleto} (@${usuario.usuario})?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    val api = MinimarketApiProvider.getApi(this@GestionUsuariosActivity)
                    val res = api.eliminarUsuario(usuario)
                    when (res) {
                        is ApiResponse.Success -> Toast.makeText(this@GestionUsuariosActivity, "Usuario eliminado", Toast.LENGTH_SHORT).show()
                        is ApiResponse.Error -> Toast.makeText(this@GestionUsuariosActivity, res.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- DIÁLOGO REGISTRAR DIRECTO CON CAMPOS EXACTOS ---

    private fun mostrarDialogoRegistrarDirecto() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_registro_usuario_admin, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etDialogNombreCompleto)
        val etUser = dialogView.findViewById<TextInputEditText>(R.id.etDialogUsuario)
        val etPass = dialogView.findViewById<TextInputEditText>(R.id.etDialogContrasena)
        val etConfirmPass = dialogView.findViewById<TextInputEditText>(R.id.etDialogConfirmarContrasena)
        val etEmail = dialogView.findViewById<TextInputEditText>(R.id.etDialogCorreo)
        val rbRolAdmin = dialogView.findViewById<RadioButton>(R.id.rbRolAdmin)
        val btnCancelar = dialogView.findViewById<MaterialButton>(R.id.btnDialogCancelar)
        val btnGuardar = dialogView.findViewById<MaterialButton>(R.id.btnDialogGuardar)

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text?.toString().orEmpty().trim()
            val user = etUser.text?.toString().orEmpty().trim()
            val pass = etPass.text?.toString().orEmpty().trim()
            val confirmPass = etConfirmPass.text?.toString().orEmpty().trim()
            val email = etEmail.text?.toString().orEmpty().trim()
            val rolSeleccionado = if (rbRolAdmin.isChecked) Constants.ROL_ADMIN else Constants.ROL_VENDEDOR

            val validacion = JsonDatabaseManager.validarUsuario(
                nombreCompleto = nombre,
                usuario = user,
                correo = email,
                contrasena = pass,
                confirmarContrasena = confirmPass
            )

            if (!validacion.isValid) {
                Toast.makeText(this, validacion.message, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val nuevoUsuario = Usuario(
                nombreCompleto = nombre,
                usuario = user,
                correo = email,
                contrasena = pass,
                rol = rolSeleccionado,
                estado = Constants.ESTADO_ACTIVO // Registro directo queda ACTIVO inmediatamente
            )

            lifecycleScope.launch {
                val api = MinimarketApiProvider.getApi(this@GestionUsuariosActivity)
                val res = api.registrarUsuario(nuevoUsuario)
                when (res) {
                    is ApiResponse.Success -> {
                        Toast.makeText(
                            this@GestionUsuariosActivity,
                            "✓ Usuario '$user' registrado directamente como $rolSeleccionado",
                            Toast.LENGTH_LONG
                        ).show()
                        dialog.dismiss()
                    }
                    is ApiResponse.Error -> {
                        Toast.makeText(this@GestionUsuariosActivity, res.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        dialog.show()
    }

    // --- ADAPTER CON DIFFUTIL ---

    class UsuarioAdapter(
        private val onAceptar: (Usuario) -> Unit,
        private val onRechazar: (Usuario) -> Unit,
        private val onAsignarRol: (Usuario) -> Unit,
        private val onToggleEstado: (Usuario) -> Unit,
        private val onCambiarRol: (Usuario) -> Unit,
        private val onMasOpciones: (Usuario) -> Unit
    ) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

        private var items: List<Usuario> = emptyList()

        fun submitList(newItems: List<Usuario>) {
            val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = items.size
                override fun getNewListSize(): Int = newItems.size
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return items[oldItemPosition].id == newItems[newItemPosition].id
                }
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return items[oldItemPosition] == newItems[newItemPosition]
                }
            })
            items = newItems
            diffResult.dispatchUpdatesTo(this)
        }

        class UsuarioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvAvatarInicial: TextView = view.findViewById(R.id.tvAvatarInicial)
            val tvNombreUsuario: TextView = view.findViewById(R.id.tvNombreUsuario)
            val tvUsername: TextView = view.findViewById(R.id.tvUsername)
            val tvRolUsuario: TextView = view.findViewById(R.id.tvRolUsuario)
            val tvEstadoUsuario: TextView = view.findViewById(R.id.tvEstadoUsuario)
            val tvCorreoUsuario: TextView = view.findViewById(R.id.tvCorreoUsuario)

            val layoutAccionesPendiente: LinearLayout = view.findViewById(R.id.layoutAccionesPendiente)
            val btnAceptarSolicitud: MaterialButton = view.findViewById(R.id.btnAceptarSolicitud)
            val btnAsignarRolSolicitud: MaterialButton = view.findViewById(R.id.btnAsignarRolSolicitud)
            val btnRechazarSolicitud: MaterialButton = view.findViewById(R.id.btnRechazarSolicitud)

            val layoutAccionesNormal: LinearLayout = view.findViewById(R.id.layoutAccionesNormal)
            val btnToggleEstado: MaterialButton = view.findViewById(R.id.btnToggleEstado)
            val btnCambiarRol: MaterialButton = view.findViewById(R.id.btnCambiarRol)
            val btnMasOpciones: ImageButton = view.findViewById(R.id.btnMasOpciones)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_usuario, parent, false)
            return UsuarioViewHolder(view)
        }

        override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
            val u = items[position]
            val context = holder.itemView.context

            // Avatar Inicial
            val inicial = if (u.nombreCompleto.isNotBlank()) u.nombreCompleto.first().uppercase() else "U"
            holder.tvAvatarInicial.text = inicial

            holder.tvNombreUsuario.text = u.nombreCompleto
            holder.tvUsername.text = "@${u.usuario}"
            holder.tvCorreoUsuario.text = "✉ ${u.correo}"

            // Rol Badge
            val rol = u.rol.uppercase()
            holder.tvRolUsuario.text = rol
            if (rol == Constants.ROL_ADMIN) {
                holder.tvRolUsuario.setBackgroundColor(ContextCompat.getColor(context, R.color.morado_scanner))
                holder.tvAvatarInicial.backgroundTintList = ContextCompat.getColorStateList(context, R.color.morado_scanner)
            } else {
                holder.tvRolUsuario.setBackgroundColor(ContextCompat.getColor(context, R.color.verde_nav))
                holder.tvAvatarInicial.backgroundTintList = ContextCompat.getColorStateList(context, R.color.verde_nav)
            }

            // Estado Badge y Layouts de Acción
            val estado = u.estado.lowercase()
            when (estado) {
                Constants.ESTADO_PENDIENTE -> {
                    holder.tvEstadoUsuario.text = "SOLICITUD PENDIENTE"
                    holder.tvEstadoUsuario.setBackgroundColor(ContextCompat.getColor(context, R.color.amarillo_accion))
                    holder.tvEstadoUsuario.setTextColor(ContextCompat.getColor(context, R.color.texto_sobre_amarillo))

                    holder.layoutAccionesPendiente.visibility = View.VISIBLE
                    holder.layoutAccionesNormal.visibility = View.GONE

                    holder.btnAceptarSolicitud.setOnClickListener { onAceptar(u) }
                    holder.btnRechazarSolicitud.setOnClickListener { onRechazar(u) }
                    holder.btnAsignarRolSolicitud.setOnClickListener { onAsignarRol(u) }
                }
                Constants.ESTADO_ACTIVO -> {
                    holder.tvEstadoUsuario.text = "ACTIVO"
                    holder.tvEstadoUsuario.setBackgroundColor(ContextCompat.getColor(context, R.color.verde_ganancia))
                    holder.tvEstadoUsuario.setTextColor(ContextCompat.getColor(context, R.color.white))

                    holder.layoutAccionesPendiente.visibility = View.GONE
                    holder.layoutAccionesNormal.visibility = View.VISIBLE

                    holder.btnToggleEstado.text = "Inhabilitar"
                    holder.btnToggleEstado.backgroundTintList = ContextCompat.getColorStateList(context, R.color.rojo_alerta)
                    holder.btnToggleEstado.setOnClickListener { onToggleEstado(u) }
                    holder.btnCambiarRol.setOnClickListener { onCambiarRol(u) }
                    holder.btnMasOpciones.setOnClickListener { onMasOpciones(u) }
                }
                else -> { // INACTIVO
                    holder.tvEstadoUsuario.text = "INHABILITADO"
                    holder.tvEstadoUsuario.setBackgroundColor(ContextCompat.getColor(context, R.color.rojo_alerta))
                    holder.tvEstadoUsuario.setTextColor(ContextCompat.getColor(context, R.color.white))

                    holder.layoutAccionesPendiente.visibility = View.GONE
                    holder.layoutAccionesNormal.visibility = View.VISIBLE

                    holder.btnToggleEstado.text = "Habilitar"
                    holder.btnToggleEstado.backgroundTintList = ContextCompat.getColorStateList(context, R.color.verde_nav)
                    holder.btnToggleEstado.setOnClickListener { onToggleEstado(u) }
                    holder.btnCambiarRol.setOnClickListener { onCambiarRol(u) }
                    holder.btnMasOpciones.setOnClickListener { onMasOpciones(u) }
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
