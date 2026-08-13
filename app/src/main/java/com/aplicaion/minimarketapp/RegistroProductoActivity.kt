package com.aplicaion.minimarketapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Categoria
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.repository.CategoriaRepository
import com.aplicaion.minimarketapp.repository.ProductoRepository
import com.aplicaion.minimarketapp.utils.Constants
import com.aplicaion.minimarketapp.utils.Resource
import com.aplicaion.minimarketapp.utils.SessionManager
import com.aplicaion.minimarketapp.viewmodel.ProductoViewModel
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream

class RegistroProductoActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var ivProducto: ImageView
    private lateinit var layoutPlaceholder: LinearLayout
    private lateinit var btnTomarFoto: MaterialButton
    private lateinit var btnGaleria: MaterialButton
    private lateinit var btnCargarUrl: MaterialButton
    private lateinit var etNombreProducto: TextInputEditText
    private lateinit var etStockActual: TextInputEditText
    private lateinit var spinnerCategoria: AutoCompleteTextView
    private lateinit var etProveedor: TextInputEditText
    private lateinit var etPrecioCompra: TextInputEditText
    private lateinit var etPrecioVenta: TextInputEditText
    private lateinit var tvGanancia: TextView
    private lateinit var etCodigoBarras: TextInputEditText
    private lateinit var btnScanner: MaterialButton
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var btnGuardar: MaterialButton
    private lateinit var bottomNavigation: BottomNavigationView

    private var selectedCategoriaId: Int = 0
    private var categoriasList: List<Categoria> = emptyList()
    private var imagenUriPath: String? = null
    private var productoEditando: Producto? = null
    private var isEditDataLoaded: Boolean = false
    private var isSavingInProgress: Boolean = false

    private var tempCameraFile: File? = null
    private var tempCameraUri: Uri? = null

    private val productoViewModel: ProductoViewModel by viewModels {
        val db = AppDatabase.getInstance(this)
        val prodRepo = ProductoRepository(db.productoDao())
        val catRepo = CategoriaRepository(db.categoriaDao())
        ProductoViewModel.Factory(prodRepo, catRepo)
    }

    private val scannerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val codigo = result.data?.getStringExtra(Constants.CODIGO_SCANEADO)
                    ?: result.data?.getStringExtra("CODIGO_SCANEADO")
                if (!codigo.isNullOrBlank()) {
                    etCodigoBarras.setText(codigo)
                    Toast.makeText(this, "Código capturado: $codigo", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // Launcher de cámara con archivo de almacenamiento persistente
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                tempCameraFile?.let { file ->
                    if (file.exists() && file.length() > 0) {
                        imagenUriPath = file.absolutePath
                        mostrarImagen(file.absolutePath)
                        Toast.makeText(this, "✓ Foto capturada y guardada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    // Fallback de cámara rápida (Thumbnail preview) en caso de que el dispositivo no use TakePicture
    private val takePreviewLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap?.let {
                try {
                    val file = obtenerArchivoDestinoPersistente()
                    FileOutputStream(file).use { out ->
                        it.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    imagenUriPath = file.absolutePath
                    mostrarImagen(file.absolutePath)
                    Toast.makeText(this, "✓ Foto guardada", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al guardar foto: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // Launcher de galería con copiado persistente al almacenamiento interno de la app
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { sourceUri ->
                try {
                    val destFile = obtenerArchivoDestinoPersistente()
                    contentResolver.openInputStream(sourceUri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (destFile.exists() && destFile.length() > 0) {
                        imagenUriPath = destFile.absolutePath
                        mostrarImagen(destFile.absolutePath)
                        Toast.makeText(this, "✓ Imagen importada correctamente", Toast.LENGTH_SHORT).show()
                    } else {
                        imagenUriPath = sourceUri.toString()
                        mostrarImagen(sourceUri.toString())
                    }
                } catch (e: Exception) {
                    imagenUriPath = sourceUri.toString()
                    mostrarImagen(sourceUri.toString())
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager.getInstance(this)
        if (!sessionManager.isAdmin) {
            Toast.makeText(this, "Acceso exclusivo para el Administrador", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Limpiar estado previo para evitar cierres accidentales al volver de cámara
        productoViewModel.resetGuardarState()

        setContentView(R.layout.activity_registro_producto)

        initViews()
        setupListeners()
        observeViewModel()

        if (savedInstanceState != null) {
            imagenUriPath = savedInstanceState.getString("KEY_IMAGEN_PATH")
            isEditDataLoaded = savedInstanceState.getBoolean("KEY_EDIT_LOADED", false)
            val savedTempPath = savedInstanceState.getString("KEY_TEMP_CAMERA_PATH")
            if (!savedTempPath.isNullOrBlank()) {
                tempCameraFile = File(savedTempPath)
            }
            if (!imagenUriPath.isNullOrBlank()) {
                mostrarImagen(imagenUriPath!!)
            }
        }

        val productoId = intent.getLongExtra("PRODUCTO_ID", intent.getIntExtra("PRODUCTO_ID", -1).toLong())
        if (productoId != -1L) {
            toolbar.title = "Editar Producto"
            btnGuardar.text = "Actualizar Producto"
            if (!isEditDataLoaded) {
                cargarProductoParaEditar(productoId)
            }
        } else {
            toolbar.title = "Nuevo Producto"
            btnGuardar.text = "Guardar Producto"
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("KEY_IMAGEN_PATH", imagenUriPath)
        outState.putBoolean("KEY_EDIT_LOADED", isEditDataLoaded)
        tempCameraFile?.let {
            outState.putString("KEY_TEMP_CAMERA_PATH", it.absolutePath)
        }
    }

    private fun obtenerArchivoDestinoPersistente(): File {
        val dir = File(filesDir, "productos_img")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "prod_${System.currentTimeMillis()}.jpg")
    }

    private fun iniciarCapturaCamara() {
        try {
            val file = obtenerArchivoDestinoPersistente()
            tempCameraFile = file
            val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
            tempCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            // Fallback en caso de que FileProvider falle
            try {
                takePreviewLauncher.launch(null)
            } catch (e2: Exception) {
                Toast.makeText(this, "No se pudo abrir la cámara: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarProductoParaEditar(id: Long) {
        productoViewModel.getById(id).observe(this) { producto ->
            if (producto == null || isEditDataLoaded) return@observe
            isEditDataLoaded = true
            productoEditando = producto
            selectedCategoriaId = producto.categoriaId

            etNombreProducto.setText(producto.nombre)
            etStockActual.setText(producto.stock.toString())
            etProveedor.setText(producto.proveedorId?.toString() ?: "")
            etPrecioCompra.setText(producto.precioCompra.toString())
            etPrecioVenta.setText(producto.precioVenta.toString())
            etCodigoBarras.setText(producto.codigoBarras)
            etDescripcion.setText(producto.descripcion ?: "")

            if (imagenUriPath.isNullOrBlank() && !producto.imagenPath.isNullOrBlank()) {
                imagenUriPath = producto.imagenPath
                mostrarImagen(producto.imagenPath)
            }

            productoViewModel.categorias.observe(this) { cats ->
                val catObj = cats.find { it.id == producto.categoriaId }
                if (catObj != null) {
                    spinnerCategoria.setText(catObj.nombre, false)
                }
            }
            calcularGanancia()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        ivProducto = findViewById(R.id.ivProducto)
        layoutPlaceholder = findViewById(R.id.layoutPlaceholder)
        btnTomarFoto = findViewById(R.id.btnTomarFoto)
        btnGaleria = findViewById(R.id.btnGaleria)
        btnCargarUrl = findViewById(R.id.btnCargarUrl)
        etNombreProducto = findViewById(R.id.etNombreProducto)
        etStockActual = findViewById(R.id.etStockActual)
        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        etProveedor = findViewById(R.id.etProveedor)
        etPrecioCompra = findViewById(R.id.etPrecioCompra)
        etPrecioVenta = findViewById(R.id.etPrecioVenta)
        tvGanancia = findViewById(R.id.tvGanancia)
        etCodigoBarras = findViewById(R.id.etCodigoBarras)
        btnScanner = findViewById(R.id.btnScanner)
        etDescripcion = findViewById(R.id.etDescripcion)
        btnGuardar = findViewById(R.id.btnGuardar)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        btnScanner.setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            scannerLauncher.launch(intent)
        }

        btnTomarFoto.setOnClickListener {
            iniciarCapturaCamara()
        }

        btnGaleria.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnCargarUrl.setOnClickListener {
            mostrarDialogoCargarUrlWeb()
        }

        val priceTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                calcularGanancia()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etPrecioCompra.addTextChangedListener(priceTextWatcher)
        etPrecioVenta.addTextChangedListener(priceTextWatcher)

        btnGuardar.setOnClickListener {
            guardarProducto()
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

    private fun observeViewModel() {
        productoViewModel.categorias.observe(this) { list ->
            categoriasList = list
            val nombres = list.map { it.nombre }
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombres)
            spinnerCategoria.setAdapter(adapter)

            spinnerCategoria.setOnItemClickListener { _, _, position, _ ->
                if (position in list.indices) {
                    selectedCategoriaId = list[position].id
                }
            }

            if (list.isNotEmpty() && selectedCategoriaId == 0) {
                spinnerCategoria.setText(list[0].nombre, false)
                selectedCategoriaId = list[0].id
            }
        }

        productoViewModel.guardarState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    btnGuardar.isEnabled = false
                }
                is Resource.Success -> {
                    btnGuardar.isEnabled = true
                    if (isSavingInProgress) {
                        isSavingInProgress = false
                        Toast.makeText(this, resource.data ?: "Producto guardado", Toast.LENGTH_SHORT).show()
                        productoViewModel.resetGuardarState()
                        finish()
                    }
                }
                is Resource.Error -> {
                    btnGuardar.isEnabled = true
                    isSavingInProgress = false
                    Toast.makeText(this, resource.message ?: "Error al guardar", Toast.LENGTH_LONG).show()
                }
                null -> {
                    btnGuardar.isEnabled = true
                }
            }
        }
    }

    private fun calcularGanancia() {
        val compra = etPrecioCompra.text?.toString()?.toDoubleOrNull() ?: 0.0
        val venta = etPrecioVenta.text?.toString()?.toDoubleOrNull() ?: 0.0
        val ganancia = venta - compra
        tvGanancia.text = "S/ %.2f".format(ganancia)
        if (ganancia >= 0) {
            tvGanancia.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.verde_ganancia))
        } else {
            tvGanancia.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.rojo_alerta))
        }
    }

    private fun guardarProducto() {
        val nombre = etNombreProducto.text?.toString()?.trim().orEmpty()
        val stock = etStockActual.text?.toString()?.trim().orEmpty()
        val categoriaText = spinnerCategoria.text?.toString()?.trim().orEmpty()
        val proveedor = etProveedor.text?.toString()?.trim().orEmpty()
        val compra = etPrecioCompra.text?.toString()?.trim().orEmpty()
        val venta = etPrecioVenta.text?.toString()?.trim().orEmpty()
        val codigo = etCodigoBarras.text?.toString()?.trim().orEmpty()
        val descripcion = etDescripcion.text?.toString()?.trim().orEmpty()

        val precioVentaVal = venta.toDoubleOrNull() ?: 0.0
        val stockVal = stock.toIntOrNull() ?: 0

        when {
            nombre.isEmpty() -> {
                etNombreProducto.error = "Requerido"
                return
            }
            stockVal < 0 -> {
                etStockActual.error = "El stock no puede ser negativo"
                return
            }
            categoriaText.isEmpty() -> {
                Toast.makeText(this, "Selecciona una categoría", Toast.LENGTH_SHORT).show()
                return
            }
            codigo.isEmpty() -> {
                etCodigoBarras.error = "Requerido"
                return
            }
            precioVentaVal <= 0 -> {
                etPrecioVenta.error = "Debe ser mayor a 0"
                return
            }
        }

        isSavingInProgress = true

        if (productoEditando != null) {
            val prodActualizado = productoEditando!!.copy(
                nombre = nombre,
                stock = stock.toIntOrNull() ?: productoEditando!!.stock,
                categoriaId = if (selectedCategoriaId > 0) selectedCategoriaId else productoEditando!!.categoriaId,
                proveedorId = proveedor.toIntOrNull() ?: productoEditando!!.proveedorId,
                precioCompra = compra.toDoubleOrNull() ?: productoEditando!!.precioCompra,
                precioVenta = precioVentaVal,
                codigoBarras = codigo,
                descripcion = descripcion,
                imagenPath = imagenUriPath ?: productoEditando!!.imagenPath
            )
            productoViewModel.actualizarProducto(prodActualizado)
        } else {
            productoViewModel.guardarProducto(
                nombre = nombre,
                stockStr = stock,
                categoriaId = selectedCategoriaId,
                proveedorNombre = proveedor,
                precioCompraStr = compra,
                precioVentaStr = venta,
                codigoBarras = codigo,
                descripcion = descripcion,
                imagenPath = imagenUriPath
            )
        }
    }

    private fun mostrarDialogoCargarUrlWeb() {
        val input = TextInputEditText(this)
        input.hint = "https://ejemplo.com/imagen.jpg"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        if (!imagenUriPath.isNullOrBlank() && (imagenUriPath!!.startsWith("http://") || imagenUriPath!!.startsWith("https://"))) {
            input.setText(imagenUriPath)
        }

        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val marginPx = (16 * resources.displayMetrics.density).toInt()
        params.setMargins(marginPx, marginPx / 2, marginPx, marginPx / 2)
        input.layoutParams = params
        container.addView(input)

        MaterialAlertDialogBuilder(this)
            .setTitle("Cargar Imagen desde la Web")
            .setMessage("Ingresa la dirección URL de la imagen en Internet:")
            .setView(container)
            .setPositiveButton("Cargar") { dialog, _ ->
                val url = input.text?.toString()?.trim().orEmpty()
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    imagenUriPath = url
                    mostrarImagen(url)
                    Toast.makeText(this, "✓ Imagen Web asignada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Ingresa una URL válida que empiece con http:// o https://", Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarImagen(path: String) {
        ivProducto.visibility = View.VISIBLE
        layoutPlaceholder.visibility = View.GONE
        Glide.with(this)
            .load(path)
            .placeholder(R.drawable.ic_product_placeholder)
            .error(R.drawable.ic_product_placeholder)
            .centerCrop()
            .into(ivProducto)
    }
}

