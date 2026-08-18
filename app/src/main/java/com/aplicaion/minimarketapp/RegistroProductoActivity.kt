package com.aplicaion.minimarketapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.aplicaion.minimarketapp.api.JsonDatabaseManager
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Categoria
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.db.entity.Proveedor
import com.aplicaion.minimarketapp.utils.Constants
import com.aplicaion.minimarketapp.utils.SessionManager
import com.aplicaion.minimarketapp.utils.formatSoles
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegistroProductoActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var ivProducto: ImageView
    private lateinit var layoutPlaceholder: LinearLayout
    private lateinit var cardImagen: MaterialCardView
    private lateinit var btnTomarFoto: MaterialButton
    private lateinit var btnGaleria: MaterialButton
    private lateinit var btnCargarUrl: MaterialButton

    private lateinit var etNombreProducto: TextInputEditText
    private lateinit var switchVentaPorPeso: SwitchMaterial
    private lateinit var layoutUnidadMedida: LinearLayout
    private lateinit var spinnerUnidadMedida: AutoCompleteTextView
    private lateinit var etStockActual: TextInputEditText
    private lateinit var spinnerCategoria: AutoCompleteTextView
    private lateinit var spinnerProveedor: AutoCompleteTextView
    private lateinit var tvRegistrarNuevoProveedorLink: TextView
    private lateinit var etPrecioCompra: TextInputEditText
    private lateinit var etPrecioVenta: TextInputEditText
    private lateinit var tvGanancia: TextView
    private lateinit var etCodigoBarras: TextInputEditText
    private lateinit var btnScanner: MaterialButton
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var btnGuardar: MaterialButton
    private lateinit var bottomNavigation: BottomNavigationView

    private var imagenUriActual: Uri? = null
    private var imagenPathParaGuardar: String? = null
    private var fotoTempFile: File? = null

    private var listaCategorias: List<Categoria> = emptyList()
    private var listaProveedores: List<Proveedor> = emptyList()
    private var categoriaSeleccionadaId: Int = 1
    private var proveedorSeleccionadoId: Int? = null

    private var editandoProductoId: Int = 0
    private var productoEnEdicion: Producto? = null

    private lateinit var sessionManager: SessionManager

    // Launcher para Escáner
    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val codigo = result.data?.getStringExtra(Constants.CODIGO_SCANEADO)
                if (!codigo.isNullOrEmpty()) {
                    etCodigoBarras.setText(codigo)
                }
            }
        }

    // Launcher para Cámara
    private val camaraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && fotoTempFile != null) {
                val uri = Uri.fromFile(fotoTempFile)
                mostrarImagenSeleccionada(uri)
                imagenPathParaGuardar = fotoTempFile!!.absolutePath
            }
        }

    // Launcher para Galería
    private val galeriaLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                mostrarImagenSeleccionada(it)
                guardarImagenDesdeUri(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_producto)

        sessionManager = SessionManager.getInstance(this)
        if (!sessionManager.isAdmin) {
            Toast.makeText(this, "Acceso restringido: Se requiere rol de Administrador", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        editandoProductoId = intent.getIntExtra("producto_id", 0)

        initViews()
        setupUnidadesMedida()
        setupListeners()
        cargarDatosDb()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        ivProducto = findViewById(R.id.ivProducto)
        layoutPlaceholder = findViewById(R.id.layoutPlaceholder)
        cardImagen = findViewById(R.id.cardImagen)
        btnTomarFoto = findViewById(R.id.btnTomarFoto)
        btnGaleria = findViewById(R.id.btnGaleria)
        btnCargarUrl = findViewById(R.id.btnCargarUrl)

        etNombreProducto = findViewById(R.id.etNombreProducto)
        switchVentaPorPeso = findViewById(R.id.switchVentaPorPeso)
        layoutUnidadMedida = findViewById(R.id.layoutUnidadMedida)
        spinnerUnidadMedida = findViewById(R.id.spinnerUnidadMedida)
        etStockActual = findViewById(R.id.etStockActual)
        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        spinnerProveedor = findViewById(R.id.spinnerProveedor)
        tvRegistrarNuevoProveedorLink = findViewById(R.id.tvRegistrarNuevoProveedorLink)
        etPrecioCompra = findViewById(R.id.etPrecioCompra)
        etPrecioVenta = findViewById(R.id.etPrecioVenta)
        tvGanancia = findViewById(R.id.tvGanancia)
        etCodigoBarras = findViewById(R.id.etCodigoBarras)
        btnScanner = findViewById(R.id.btnScanner)
        etDescripcion = findViewById(R.id.etDescripcion)
        btnGuardar = findViewById(R.id.btnGuardar)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        if (editandoProductoId > 0) {
            toolbar.title = "Editar Producto"
            btnGuardar.text = "Actualizar Producto"
        } else {
            toolbar.title = "Registro de Producto"
            btnGuardar.text = "Guardar Producto"
        }

        toolbar.setNavigationOnClickListener { finish() }
        bottomNavigation.selectedItemId = R.id.nav_inventario
    }

    private fun setupUnidadesMedida() {
        val unidades = listOf("KG - Kilogramos", "UND - Unidad", "PAQ - Paquete", "G - Gramos")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, unidades)
        spinnerUnidadMedida.setAdapter(adapter)
        spinnerUnidadMedida.setText("KG - Kilogramos", false)

        layoutUnidadMedida.visibility = View.GONE
        switchVentaPorPeso.setOnCheckedChangeListener { _, isChecked ->
            layoutUnidadMedida.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupListeners() {
        btnTomarFoto.setOnClickListener { abrirCamara() }
        btnGaleria.setOnClickListener { galeriaLauncher.launch("image/*") }
        btnCargarUrl.setOnClickListener { mostrarDialogUrl() }

        btnScanner.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            scannerLauncher.launch(intent)
        }

        tvRegistrarNuevoProveedorLink.setOnClickListener {
            val intent = Intent(this, ProveedorActivity::class.java)
            startActivity(intent)
        }

        val watcherCalculoGanancia = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calcularGanancia()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etPrecioCompra.addTextChangedListener(watcherCalculoGanancia)
        etPrecioVenta.addTextChangedListener(watcherCalculoGanancia)

        btnGuardar.setOnClickListener {
            guardarOActualizarProducto()
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
                    val intent = Intent(this, InventarioMainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
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

    private fun cargarDatosDb() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            // Categorías
            db.categoriaDao().getAll().collect { cats ->
                listaCategorias = cats
                val catNombres = cats.map { it.nombre }
                val catAdapter = ArrayAdapter(this@RegistroProductoActivity, android.R.layout.simple_dropdown_item_1line, catNombres)
                spinnerCategoria.setAdapter(catAdapter)
                spinnerCategoria.setOnItemClickListener { _, _, position, _ ->
                    categoriaSeleccionadaId = cats[position].id
                }

                val preselectedCatId = intent.getIntExtra("preselected_categoria_id", 0)
                if (preselectedCatId > 0) {
                    val match = cats.firstOrNull { it.id == preselectedCatId }
                    if (match != null) {
                        categoriaSeleccionadaId = match.id
                        spinnerCategoria.setText(match.nombre, false)
                    }
                }
            }
        }

        lifecycleScope.launch {
            // Proveedores Registrados
            db.proveedorDao().getAll().collect { provs ->
                listaProveedores = provs
                val provNombres = mutableListOf("Ninguno / Sin Proveedor")
                provs.forEach { p ->
                    provNombres.add("[RUC ${p.ruc}] ${p.nombre}")
                }
                val provAdapter = ArrayAdapter(this@RegistroProductoActivity, android.R.layout.simple_dropdown_item_1line, provNombres)
                spinnerProveedor.setAdapter(provAdapter)
                spinnerProveedor.setOnItemClickListener { _, _, position, _ ->
                    proveedorSeleccionadoId = if (position == 0) null else provs[position - 1].id
                }
            }
        }

        if (editandoProductoId > 0) {
            lifecycleScope.launch {
                val prod = db.productoDao().getByIdSync(editandoProductoId)
                if (prod != null) {
                    productoEnEdicion = prod
                    poblarFormulario(prod)
                }
            }
        }
    }

    private fun poblarFormulario(prod: Producto) {
        etNombreProducto.setText(prod.nombre)
        etStockActual.setText(prod.stock.toString())
        etPrecioCompra.setText(String.format(Locale.US, "%.2f", prod.precioCompra))
        etPrecioVenta.setText(String.format(Locale.US, "%.2f", prod.precioVenta))
        etCodigoBarras.setText(prod.codigoBarras)
        etDescripcion.setText(prod.descripcion)

        val esPorPeso = prod.esPorPeso || prod.tipoVenta == "PESO" || prod.unidadMedida == "KG"
        switchVentaPorPeso.isChecked = esPorPeso
        layoutUnidadMedida.visibility = if (esPorPeso) View.VISIBLE else View.GONE
        spinnerUnidadMedida.setText(if (prod.unidadMedida == "KG") "KG - Kilogramos" else prod.unidadMedida, false)

        categoriaSeleccionadaId = prod.categoriaId
        val cat = listaCategorias.firstOrNull { it.id == prod.categoriaId }
        if (cat != null) {
            spinnerCategoria.setText(cat.nombre, false)
        }

        proveedorSeleccionadoId = prod.proveedorId
        if (prod.proveedorId != null) {
            val prov = listaProveedores.firstOrNull { it.id == prod.proveedorId }
            if (prov != null) {
                spinnerProveedor.setText("[RUC ${prov.ruc}] ${prov.nombre}", false)
            }
        } else {
            spinnerProveedor.setText("Ninguno / Sin Proveedor", false)
        }

        if (!prod.imagenPath.isNullOrBlank()) {
            imagenPathParaGuardar = prod.imagenPath
            mostrarImagenPath(prod.imagenPath)
        }
        calcularGanancia()
    }

    private fun calcularGanancia() {
        val compra = etPrecioCompra.text.toString().toDoubleOrNull() ?: 0.0
        val venta = etPrecioVenta.text.toString().toDoubleOrNull() ?: 0.0
        val ganancia = venta - compra
        tvGanancia.text = ganancia.formatSoles()
        if (ganancia >= 0) {
            tvGanancia.setTextColor(ContextCompat.getColor(this, R.color.verde_ganancia))
        } else {
            tvGanancia.setTextColor(ContextCompat.getColor(this, R.color.rojo_alerta))
        }
    }

    private fun guardarOActualizarProducto() {
        val nombre = etNombreProducto.text.toString().trim()
        val stockStr = etStockActual.text.toString().trim()
        val precioCompraStr = etPrecioCompra.text.toString().trim()
        val precioVentaStr = etPrecioVenta.text.toString().trim()
        val codigo = etCodigoBarras.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()

        val stock = stockStr.toIntOrNull()
        val precioCompra = precioCompraStr.toDoubleOrNull()
        val precioVenta = precioVentaStr.toDoubleOrNull()

        val validacion = JsonDatabaseManager.validarProducto(
            nombre = nombre,
            codigoBarras = codigo,
            stock = stock,
            precioCompra = precioCompra,
            precioVenta = precioVenta,
            categoriaId = categoriaSeleccionadaId
        )

        if (!validacion.isValid) {
            Toast.makeText(this, validacion.message, Toast.LENGTH_LONG).show()
            return
        }

        val esPorPeso = switchVentaPorPeso.isChecked
        val tipoVenta = if (esPorPeso) "PESO" else "UNIDAD"
        val unidadMedida = if (esPorPeso) "KG" else "UND"

        val nuevoProducto = Producto(
            id = editandoProductoId,
            nombre = nombre,
            categoriaId = categoriaSeleccionadaId,
            precioCompra = precioCompra ?: 0.0,
            precioVenta = precioVenta ?: 0.0,
            stock = stock ?: 0,
            descripcion = descripcion,
            codigoBarras = codigo,
            proveedorId = proveedorSeleccionadoId,
            imagenPath = imagenPathParaGuardar,
            esPorPeso = esPorPeso,
            unidadMedida = unidadMedida,
            tipoVenta = tipoVenta
        )

        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            try {
                // Verificar código de barras duplicado
                val existente = db.productoDao().getByCodigoSync(codigo)
                if (existente != null && existente.id != editandoProductoId) {
                    Toast.makeText(this@RegistroProductoActivity, "Ya existe otro producto con el código '$codigo'", Toast.LENGTH_LONG).show()
                    return@launch
                }

                if (editandoProductoId > 0) {
                    db.productoDao().update(nuevoProducto)
                    JsonDatabaseManager.syncAndSaveJsonFile(this@RegistroProductoActivity, db)
                    Toast.makeText(this@RegistroProductoActivity, "✓ Producto actualizado correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    db.productoDao().insert(nuevoProducto)
                    JsonDatabaseManager.syncAndSaveJsonFile(this@RegistroProductoActivity, db)
                    Toast.makeText(this@RegistroProductoActivity, "✓ Producto registrado exitosamente", Toast.LENGTH_SHORT).show()
                }
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@RegistroProductoActivity, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun abrirCamara() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "IMG_${timeStamp}_"
        val storageDir = cacheDir
        val image = File.createTempFile(imageFileName, ".jpg", storageDir)
        fotoTempFile = image

        val photoURI = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            image
        )
        camaraLauncher.launch(photoURI)
    }

    private fun mostrarImagenSeleccionada(uri: Uri) {
        imagenUriActual = uri
        layoutPlaceholder.visibility = View.GONE
        ivProducto.visibility = View.VISIBLE
        Glide.with(this).load(uri).centerCrop().into(ivProducto)
    }

    private fun mostrarImagenPath(path: String) {
        layoutPlaceholder.visibility = View.GONE
        ivProducto.visibility = View.VISIBLE
        Glide.with(this).load(path).centerCrop().into(ivProducto)
    }

    private fun guardarImagenDesdeUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "prod_${System.currentTimeMillis()}.jpg")
            val outputStream = file.outputStream()
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            imagenPathParaGuardar = file.absolutePath
        } catch (e: Exception) {
            imagenPathParaGuardar = uri.toString()
        }
    }

    private fun mostrarDialogUrl() {
        val input = TextInputEditText(this).apply {
            hint = "https://ejemplo.com/imagen.jpg"
            maxLines = 1
        }
        AlertDialog.Builder(this)
            .setTitle("Cargar Imagen por URL")
            .setView(input)
            .setPositiveButton("Cargar") { _, _ ->
                val url = input.text.toString().trim()
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    imagenPathParaGuardar = url
                    mostrarImagenPath(url)
                } else {
                    Toast.makeText(this, "Ingrese una URL válida (http:// o https://)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
