package com.aplicaion.minimarketapp.api

import android.content.Context
import android.util.Log
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Categoria
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.db.entity.Proveedor
import com.aplicaion.minimarketapp.db.entity.Usuario
import com.aplicaion.minimarketapp.db.entity.Venta
import com.aplicaion.minimarketapp.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader

/**
 * Gestor de Base de Datos JSON para Minimarket App.
 * Permite sincronizar y respaldar en tiempo real todos los datos en archivos JSON.
 */
object JsonDatabaseManager {

    private const val TAG = "JsonDatabaseManager"
    private const val ASSET_FILE_NAME = "minimarket_database.json"
    private const val LIVE_FILE_NAME = "minimarket_database_live.json"

    data class DatabaseBackup(
        val version: Int,
        val appName: String,
        val generatedAt: String,
        val usuarios: List<Usuario>,
        val categorias: List<Categoria>,
        val proveedores: List<Proveedor>,
        val productos: List<Producto>,
        val ventas: List<Venta> = emptyList()
    )

    /**
     * Lee el JSON de assets o del archivo local modificado más reciente.
     */
    fun readJsonDatabase(context: Context): String {
        val liveFile = File(context.filesDir, LIVE_FILE_NAME)
        if (liveFile.exists() && liveFile.length() > 0) {
            try {
                val fis = FileInputStream(liveFile)
                val reader = BufferedReader(InputStreamReader(fis, Charsets.UTF_8))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                return sb.toString()
            } catch (e: Exception) {
                Log.e(TAG, "Error al leer base de datos JSON viva", e)
            }
        }
        return readJsonFromAssets(context, ASSET_FILE_NAME)
    }

    /**
     * Lee el archivo JSON original desde assets.
     */
    fun readJsonFromAssets(context: Context, fileName: String = ASSET_FILE_NAME): String {
        return try {
            val inputStream = context.assets.open(fileName)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val stringBuilder = StringBuilder()
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            bufferedReader.close()
            stringBuilder.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error al leer $fileName desde assets", e)
            ""
        }
    }

    /**
     * Parsea un string JSON completo a objetos estructurados.
     */
    fun parseJsonDatabase(jsonString: String): DatabaseBackup {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        val appName = root.optString("appName", "Minimarket")
        val generatedAt = root.optString("generatedAt", "")

        val usuariosList = mutableListOf<Usuario>()
        val categoriasList = mutableListOf<Categoria>()
        val proveedoresList = mutableListOf<Proveedor>()
        val productosList = mutableListOf<Producto>()
        val ventasList = mutableListOf<Venta>()

        // 1. Usuarios
        val usuariosArray = root.optJSONArray("usuarios") ?: JSONArray()
        for (i in 0 until usuariosArray.length()) {
            val u = usuariosArray.getJSONObject(i)
            usuariosList.add(
                Usuario(
                    id = u.optInt("id", 0),
                    nombreCompleto = u.optString("nombreCompleto", ""),
                    usuario = u.optString("usuario", ""),
                    correo = u.optString("correo", ""),
                    contrasena = u.optString("contrasena", ""),
                    rol = u.optString("rol", Constants.ROL_VENDEDOR),
                    estado = u.optString("estado", Constants.ESTADO_ACTIVO)
                )
            )
        }

        // 2. Categorías
        val categoriasArray = root.optJSONArray("categorias") ?: JSONArray()
        for (i in 0 until categoriasArray.length()) {
            val c = categoriasArray.getJSONObject(i)
            categoriasList.add(
                Categoria(
                    id = c.optInt("id", 0),
                    nombre = c.optString("nombre", ""),
                    descripcion = c.optString("descripcion", "")
                )
            )
        }

        // 3. Proveedores
        val proveedoresArray = root.optJSONArray("proveedores") ?: JSONArray()
        for (i in 0 until proveedoresArray.length()) {
            val p = proveedoresArray.getJSONObject(i)
            proveedoresList.add(
                Proveedor(
                    id = p.optInt("id", 0),
                    nombre = p.optString("nombre", ""),
                    ruc = p.optString("ruc", ""),
                    celular = p.optString("celular", ""),
                    direccion = p.optString("direccion", ""),
                    correo = p.optString("correo", ""),
                    estado = p.optString("estado", "activo")
                )
            )
        }

        // 4. Productos
        val productosArray = root.optJSONArray("productos") ?: JSONArray()
        for (i in 0 until productosArray.length()) {
            val prod = productosArray.getJSONObject(i)
            val img = if (prod.has("imagenPath") && !prod.isNull("imagenPath")) {
                prod.optString("imagenPath")
            } else null

            val provId = if (prod.has("proveedorId") && !prod.isNull("proveedorId")) {
                prod.optInt("proveedorId")
            } else null

            val esPorPeso = prod.optBoolean("esPorPeso", false)
            val unidadMedida = prod.optString("unidadMedida", if (esPorPeso) "KG" else "UND")
            val tipoVenta = prod.optString("tipoVenta", if (esPorPeso) "PESO" else "UNIDAD")

            productosList.add(
                Producto(
                    id = prod.optInt("id", 0),
                    nombre = prod.optString("nombre", ""),
                    categoriaId = prod.optInt("categoriaId", 1),
                    precioCompra = prod.optDouble("precioCompra", 0.0).coerceAtLeast(0.0),
                    precioVenta = prod.optDouble("precioVenta", 0.0).coerceAtLeast(0.0),
                    stock = prod.optInt("stock", 0).coerceAtLeast(0), // Validación: nunca negativo
                    descripcion = prod.optString("descripcion", ""),
                    codigoBarras = prod.optString("codigoBarras", ""),
                    proveedorId = provId,
                    imagenPath = img,
                    esPorPeso = esPorPeso,
                    unidadMedida = unidadMedida,
                    tipoVenta = tipoVenta
                )
            )
        }

        // 5. Ventas
        val ventasArray = root.optJSONArray("ventas") ?: JSONArray()
        for (i in 0 until ventasArray.length()) {
            val v = ventasArray.getJSONObject(i)
            ventasList.add(
                Venta(
                    id = v.optInt("id", 0),
                    fecha = v.optLong("fecha", System.currentTimeMillis()),
                    codigoVenta = v.optString("codigoVenta", ""),
                    subtotal = v.optDouble("subtotal", 0.0),
                    igv = v.optDouble("igv", 0.0),
                    total = v.optDouble("total", 0.0),
                    metodoPago = v.optString("metodoPago", "EFECTIVO"),
                    estado = v.optString("estado", "COMPLETADA")
                )
            )
        }

        return DatabaseBackup(
            version = version,
            appName = appName,
            generatedAt = generatedAt,
            usuarios = usuariosList,
            categorias = categoriasList,
            proveedores = proveedoresList,
            productos = productosList,
            ventas = ventasList
        )
    }

    /**
     * Puebla la base de datos Room a partir del JSON con validación de datos.
     */
    suspend fun seedDatabaseFromAssets(context: Context, db: AppDatabase) = withContext(Dispatchers.IO) {
        val jsonStr = readJsonDatabase(context)
        if (jsonStr.isNotBlank()) {
            try {
                val data = parseJsonDatabase(jsonStr)

                val usuarioDao = db.usuarioDao()
                data.usuarios.forEach { user ->
                    val existing = usuarioDao.getByUsuario(user.usuario)
                    if (existing == null) {
                        usuarioDao.insert(user)
                    }
                }

                val categoriaDao = db.categoriaDao()
                if (categoriaDao.getCount() == 0) {
                    data.categorias.forEach { cat ->
                        categoriaDao.insert(cat)
                    }
                }

                val proveedorDao = db.proveedorDao()
                data.proveedores.forEach { prov ->
                    val existing = proveedorDao.getByRuc(prov.ruc)
                    if (existing == null) {
                        proveedorDao.insert(prov)
                    }
                }

                val productoDao = db.productoDao()
                if (productoDao.getCount() == 0) {
                    data.productos.forEach { prod ->
                        val prodValido = prod.copy(
                            stock = prod.stock.coerceAtLeast(0),
                            precioCompra = prod.precioCompra.coerceAtLeast(0.0),
                            precioVenta = prod.precioVenta.coerceAtLeast(0.01)
                        )
                        productoDao.insert(prodValido)
                    }
                }
                Log.d(TAG, "Base de datos Room sincronizada con JSON")
            } catch (e: Exception) {
                Log.e(TAG, "Error al poblar la base de datos desde JSON", e)
            }
        }
    }

    /**
     * Guarda y sincroniza la base de datos Room en el archivo JSON local en tiempo real.
     */
    suspend fun syncAndSaveJsonFile(context: Context, db: AppDatabase) = withContext(Dispatchers.IO) {
        try {
            val json = exportRoomToJson(db)
            val liveFile = File(context.filesDir, LIVE_FILE_NAME)
            val fos = FileOutputStream(liveFile)
            fos.write(json.toByteArray(Charsets.UTF_8))
            fos.flush()
            fos.close()
            Log.d(TAG, "Base de datos JSON viva actualizada exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar base de datos JSON viva", e)
        }
    }

    /**
     * Exporta toda la base de datos Room a un JSON formateado.
     */
    suspend fun exportRoomToJson(db: AppDatabase): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "Minimarket App Database")
        root.put(
            "generatedAt",
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date())
        )

        // Usuarios
        val usuariosArray = JSONArray()
        db.usuarioDao().getAllList().forEach { u ->
            val o = JSONObject()
            o.put("id", u.id)
            o.put("nombreCompleto", u.nombreCompleto)
            o.put("usuario", u.usuario)
            o.put("correo", u.correo)
            o.put("contrasena", u.contrasena)
            o.put("rol", u.rol)
            o.put("estado", u.estado)
            usuariosArray.put(o)
        }
        root.put("usuarios", usuariosArray)

        // Categorías
        val categoriasArray = JSONArray()
        db.categoriaDao().getAllList().forEach { cat ->
            val o = JSONObject()
            o.put("id", cat.id)
            o.put("nombre", cat.nombre)
            o.put("descripcion", cat.descripcion)
            categoriasArray.put(o)
        }
        root.put("categorias", categoriasArray)

        // Proveedores
        val proveedoresArray = JSONArray()
        db.proveedorDao().getAllList().forEach { prov ->
            val o = JSONObject()
            o.put("id", prov.id)
            o.put("nombre", prov.nombre)
            o.put("ruc", prov.ruc)
            o.put("celular", prov.celular)
            o.put("direccion", prov.direccion)
            o.put("correo", prov.correo)
            o.put("estado", prov.estado)
            proveedoresArray.put(o)
        }
        root.put("proveedores", proveedoresArray)

        // Productos
        val productosArray = JSONArray()
        db.productoDao().getAllList().forEach { prod ->
            val o = JSONObject()
            o.put("id", prod.id)
            o.put("nombre", prod.nombre)
            o.put("categoriaId", prod.categoriaId)
            o.put("precioCompra", prod.precioCompra)
            o.put("precioVenta", prod.precioVenta)
            o.put("stock", prod.stock)
            o.put("descripcion", prod.descripcion)
            o.put("codigoBarras", prod.codigoBarras)
            o.put("proveedorId", prod.proveedorId ?: JSONObject.NULL)
            o.put("imagenPath", prod.imagenPath ?: JSONObject.NULL)
            o.put("esPorPeso", prod.esPorPeso)
            o.put("unidadMedida", prod.unidadMedida)
            o.put("tipoVenta", prod.tipoVenta)
            productosArray.put(o)
        }
        root.put("productos", productosArray)

        // Ventas
        val ventasArray = JSONArray()
        db.ventaDao().getAllList().forEach { v ->
            val o = JSONObject()
            o.put("id", v.id)
            o.put("fecha", v.fecha)
            o.put("codigoVenta", v.codigoVenta)
            o.put("subtotal", v.subtotal)
            o.put("igv", v.igv)
            o.put("total", v.total)
            o.put("metodoPago", v.metodoPago)
            o.put("estado", v.estado)
            ventasArray.put(o)
        }
        root.put("ventas", ventasArray)

        return@withContext root.toString(2)
    }

    /**
     * Validador centralizado para Productos
     */
    fun validarProducto(
        nombre: String,
        codigoBarras: String,
        stock: Int?,
        precioCompra: Double?,
        precioVenta: Double?,
        categoriaId: Int
    ): ValidationResult {
        if (nombre.trim().isEmpty()) {
            return ValidationResult(false, "El nombre del producto es obligatorio")
        }
        if (codigoBarras.trim().isEmpty()) {
            return ValidationResult(false, "El código de barras es obligatorio")
        }
        if (stock == null || stock < 0) {
            return ValidationResult(false, "El stock no puede ser negativo (mínimo 0)")
        }
        if (precioCompra == null || precioCompra < 0.0) {
            return ValidationResult(false, "El precio de compra no puede ser negativo")
        }
        if (precioVenta == null || precioVenta <= 0.0) {
            return ValidationResult(false, "El precio de venta debe ser mayor a 0")
        }
        if (categoriaId <= 0) {
            return ValidationResult(false, "Debe seleccionar una categoría válida")
        }
        return ValidationResult(true, "Válido")
    }

    /**
     * Validador centralizado para Proveedores
     */
    fun validarProveedor(
        nombre: String,
        ruc: String,
        celular: String,
        correo: String,
        direccion: String = ""
    ): ValidationResult {
        val nom = nombre.trim()
        if (nom.length < 2) {
            return ValidationResult(false, "El nombre del proveedor debe tener al menos 2 caracteres")
        }
        val rucTrim = ruc.trim()
        if (rucTrim.isEmpty()) {
            return ValidationResult(false, "El RUC es obligatorio")
        }
        if (!rucTrim.all { it.isDigit() } || (rucTrim.length != 11 && rucTrim.length != 8)) {
            return ValidationResult(false, "El RUC debe tener 11 dígitos numéricos (o DNI de 8 dígitos)")
        }
        val celTrim = celular.trim().replace(" ", "").replace("-", "")
        if (celTrim.isEmpty()) {
            return ValidationResult(false, "El número de celular o teléfono es obligatorio")
        }
        if (!celTrim.all { it.isDigit() } || celTrim.length < 7 || celTrim.length > 12) {
            return ValidationResult(false, "Ingrese un número telefónico válido (ej. 987654321)")
        }
        val correoTrim = correo.trim()
        if (correoTrim.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(correoTrim).matches()) {
            return ValidationResult(false, "El formato del correo electrónico no es válido")
        }
        return ValidationResult(true, "Válido")
    }

    /**
     * Validador centralizado para Usuarios y Credenciales Seguras
     */
    fun validarUsuario(
        nombreCompleto: String,
        usuario: String,
        correo: String,
        contrasena: String,
        confirmarContrasena: String? = null
    ): ValidationResult {
        val nom = nombreCompleto.trim()
        if (nom.length < 3) {
            return ValidationResult(false, "El nombre completo debe tener al menos 3 caracteres")
        }

        val userTrim = usuario.trim()
        if (userTrim.length < 3 || userTrim.length > 25) {
            return ValidationResult(false, "El nombre de usuario debe tener entre 3 y 25 caracteres")
        }
        if (userTrim.contains(" ")) {
            return ValidationResult(false, "El nombre de usuario no puede contener espacios")
        }
        val userRegex = Regex("^[a-zA-Z0-9_.]+$")
        if (!userRegex.matches(userTrim)) {
            return ValidationResult(false, "El usuario solo puede contener letras, números, puntos o guiones bajos")
        }

        val correoTrim = correo.trim()
        if (correoTrim.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(correoTrim).matches()) {
            return ValidationResult(false, "Ingrese un correo electrónico válido (ejemplo@dominio.com)")
        }

        // Validación de Contraseña Segura
        if (contrasena.length < 6) {
            return ValidationResult(false, "La contraseña debe tener al menos 6 caracteres")
        }
        val hasLetter = contrasena.any { it.isLetter() }
        val hasDigit = contrasena.any { it.isDigit() }
        if (!hasLetter || !hasDigit) {
            return ValidationResult(false, "Contraseña poco segura: debe combinar al menos una letra y un número")
        }

        if (confirmarContrasena != null && contrasena != confirmarContrasena) {
            return ValidationResult(false, "Las contraseñas no coinciden. Verifíquelas nuevamente.")
        }
        return ValidationResult(true, "Válido")
    }

    data class ValidationResult(
        val isValid: Boolean,
        val message: String
    )
}
