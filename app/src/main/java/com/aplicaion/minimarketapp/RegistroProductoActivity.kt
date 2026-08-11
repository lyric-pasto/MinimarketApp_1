package com.aplicaion.minimarketapp

import android.app.Activity
import android.content.Intent
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
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Categoria
import com.aplicaion.minimarketapp.repository.CategoriaRepository
import com.aplicaion.minimarketapp.repository.ProductoRepository
import com.aplicaion.minimarketapp.utils.Constants
import com.aplicaion.minimarketapp.utils.Resource
import com.aplicaion.minimarketapp.utils.formatSoles
import com.aplicaion.minimarketapp.viewmodel.ProductoViewModel
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

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
    private var productoEditando: com.aplicaion.minimarketapp.db.entity.Producto? = null

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
                if (!codigo.isNull_or_blank_safe()) {
                    etCodigoBarras.setText(codigo)
                    Toast.makeText(this, "Código capturado: $codigo", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: android.graphics.Bitmap? ->
            bitmap?.let {
                try {
                    val file = java.io.File(cacheDir, "prod_${System.currentTimeMillis()}.jpg")
                    java.io.FileOutputStream(file).use { out ->
                        it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    imagenUriPath = file.absolutePath
                    mostrarImagen(file.absolutePath)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error guardando foto", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    val file = java.io.File(filesDir, "prod_${System.currentTimeMillis()}.jpg")
                    contentResolver.openInputStream(it)?.use { input ->
                        java.io.FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    imagenUriPath = file.absolutePath
                    mostrarImagen(file.absolutePath)
                } catch (e: Exception) {
                    imagenUriPath = it.toString()
                    mostrarImagen(it.toString())
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_producto)

        initViews()
        setupListeners()
        observeViewModel()

        val productoId = intent.getLongExtra("PRODUCTO_ID", intent.getIntExtra("PRODUCTO_ID", -1).toLong())
        if (productoId != -1L) {
            toolbar.title = "Editar Producto"
            btnGuardar.text = "Actualizar"
            cargarProductoParaEditar(productoId)
        } else {
            toolbar.title = "Nuevo Producto"
        }
    }

    private fun cargarProductoParaEditar(id: Long) {
        productoViewModel.getById(id).observe(this) { producto ->
            if (producto == null) return@observe
            productoEditando = producto
            selectedCategoriaId = producto.categoriaId

            etNombreProducto.setText(producto.nombre)
            etStockActual.setText(producto.stock.toString())
            etProveedor.setText(producto.proveedorId?.toString() ?: "")
            etPrecioCompra.setText(producto.precioCompra.toString())
            etPrecioVenta.setText(producto.precioVenta.toString())
            etCodigoBarras.setText(producto.codigoBarras)
            etDescripcion.setText(producto.descripcion ?: "")

            if (!producto.imagenPath.isNull_or_blank_safe()) {
                imagenUriPath = producto.imagenPath
                mostrarImagen(producto.imagenPath!!)
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
            cameraLauncher.launch(null)
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
                R.id.nav_inventario -> true
                R.id.nav_historial -> {
                    val intent = Intent(this, HistorialVentaActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_proveedores -> {
                    val intent = Intent(this, ProveedorActivity::class.java)
                    startActivity(intent)
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
                    Toast.makeText(this, resource.data ?: "Producto guardado", Toast.LENGTH_SHORT).show()
                    productoViewModel.resetGuardarState()
                    finish()
                }
                is Resource.Error -> {
                    btnGuardar.isEnabled = true
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
                Toast.makeText(this, "Seleccioná una categoría", Toast.LENGTH_SHORT).show()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val codigo = data?.getStringExtra(Constants.CODIGO_SCANEADO)
                ?: data?.getStringExtra("CODIGO_SCANEADO")
            if (!codigo.isNull_or_blank_safe()) {
                etCodigoBarras.setText(codigo)
                Toast.makeText(this, "Código capturado: $codigo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoCargarUrlWeb() {
        val input = com.google.android.material.textfield.TextInputEditText(this)
        input.hint = "https://ejemplo.com/imagen.jpg"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        if (!imagenUriPath.isNull_or_blank_safe() && (imagenUriPath!!.startsWith("http://") || imagenUriPath!!.startsWith("https://"))) {
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

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Cargar Imagen desde la Web")
            .setMessage("Ingresa la dirección URL de la imagen en Internet:")
            .setView(container)
            .setPositiveButton("Cargar") { dialog, _ ->
                val url = input.text?.toString()?.trim().orEmpty()
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    imagenUriPath = url
                    mostrarImagen(url)
                    Toast.makeText(this, "Imagen Web asignada", Toast.LENGTH_SHORT).show()
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

    private fun String?.isNull_or_blank_safe(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}
