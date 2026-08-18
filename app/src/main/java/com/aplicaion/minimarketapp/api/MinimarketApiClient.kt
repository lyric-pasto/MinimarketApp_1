package com.aplicaion.minimarketapp.api

import android.content.Context
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Categoria
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.db.entity.Proveedor
import com.aplicaion.minimarketapp.db.entity.Usuario
import com.aplicaion.minimarketapp.viewmodel.ItemCarrito
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cliente API para el Sistema Minimarket.
 * Proporciona métodos unificados para interactuar con la Base de Datos JSON y Room,
 * aplicando validaciones de negocio en tiempo real.
 */
class MinimarketApiClient(private val context: Context) {

    private val db: AppDatabase = AppDatabase.getInstance(context)
    private val productoDao = db.productoDao()
    private val categoriaDao = db.categoriaDao()
    private val proveedorDao = db.proveedorDao()
    private val usuarioDao = db.usuarioDao()
    private val ventaDao = db.ventaDao()
    private val detalleVentaDao = db.detalleVentaDao()

    sealed class ApiResponse<out T> {
        data class Success<out T>(val data: T, val message: String = "Operación exitosa") : ApiResponse<T>()
        data class Error(val message: String, val code: Int = 400) : ApiResponse<Nothing>()
    }

    // ==========================================
    // API PRODUCTOS
    // ==========================================

    suspend fun getProductos(): ApiResponse<List<Producto>> = withContext(Dispatchers.IO) {
        try {
            val list = productoDao.getAllList()
            ApiResponse.Success(list)
        } catch (e: Exception) {
            ApiResponse.Error("Error al obtener productos: ${e.localizedMessage}")
        }
    }

    suspend fun getProductoPorCodigoBarras(codigo: String): ApiResponse<Producto> = withContext(Dispatchers.IO) {
        try {
            if (codigo.isBlank()) {
                return@withContext ApiResponse.Error("Código de barras inválido")
            }
            val prod = productoDao.getByCodigo(codigo.trim())
            if (prod != null) {
                ApiResponse.Success(prod)
            } else {
                ApiResponse.Error("Producto con código '$codigo' no encontrado", 404)
            }
        } catch (e: Exception) {
            ApiResponse.Error("Error al buscar producto: ${e.localizedMessage}")
        }
    }

    suspend fun guardarProducto(producto: Producto): ApiResponse<Long> = withContext(Dispatchers.IO) {
        try {
            val validacion = JsonDatabaseManager.validarProducto(
                nombre = producto.nombre,
                codigoBarras = producto.codigoBarras,
                stock = producto.stock,
                precioCompra = producto.precioCompra,
                precioVenta = producto.precioVenta,
                categoriaId = producto.categoriaId
            )
            if (!validacion.isValid) {
                return@withContext ApiResponse.Error(validacion.message)
            }

            // Validar unicidad del código de barras si es nuevo o cambio
            val existente = productoDao.getByCodigo(producto.codigoBarras.trim())
            if (existente != null && existente.id != producto.id) {
                return@withContext ApiResponse.Error("El código de barras ya pertenece a '${existente.nombre}'")
            }

            val id = if (producto.id > 0) {
                productoDao.actualizar(producto)
                producto.id.toLong()
            } else {
                productoDao.insert(producto)
            }
            ApiResponse.Success(id, "Producto guardado correctamente")
        } catch (e: Exception) {
            ApiResponse.Error("Error al guardar producto: ${e.localizedMessage}")
        }
    }

    // ==========================================
    // API PROVEEDORES
    // ==========================================

    suspend fun getProveedores(): ApiResponse<List<Proveedor>> = withContext(Dispatchers.IO) {
        try {
            val list = proveedorDao.getAllList()
            ApiResponse.Success(list)
        } catch (e: Exception) {
            ApiResponse.Error("Error al obtener proveedores: ${e.localizedMessage}")
        }
    }

    suspend fun guardarProveedor(proveedor: Proveedor): ApiResponse<Long> = withContext(Dispatchers.IO) {
        try {
            val validacion = JsonDatabaseManager.validarProveedor(
                nombre = proveedor.nombre,
                ruc = proveedor.ruc,
                celular = proveedor.celular,
                correo = proveedor.correo
            )
            if (!validacion.isValid) {
                return@withContext ApiResponse.Error(validacion.message)
            }

            val existente = proveedorDao.getByRuc(proveedor.ruc.trim())
            if (existente != null && existente.id != proveedor.id) {
                return@withContext ApiResponse.Error("El RUC '${proveedor.ruc}' ya se encuentra registrado")
            }

            val id = if (proveedor.id > 0) {
                proveedorDao.update(proveedor)
                proveedor.id.toLong()
            } else {
                proveedorDao.insert(proveedor)
            }
            ApiResponse.Success(id, "Proveedor guardado correctamente")
        } catch (e: Exception) {
            ApiResponse.Error("Error al guardar proveedor: ${e.localizedMessage}")
        }
    }

    // ==========================================
    // API VENTAS Y VALIDACIONES
    // ==========================================

    suspend fun registrarVenta(items: List<ItemCarrito>, metodoPago: String): ApiResponse<Long> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) {
            return@withContext ApiResponse.Error("El carrito está vacío")
        }

        // 1. Validar existencias de stock de cada producto
        for (item in items) {
            val currentProd = productoDao.getByIdSync(item.producto.id)
            if (currentProd == null) {
                return@withContext ApiResponse.Error("Producto '${item.producto.nombre}' no encontrado")
            }
            if (currentProd.stock < item.cantidad) {
                return@withContext ApiResponse.Error(
                    "Stock insuficiente para '${currentProd.nombre}'. Stock disponible: ${currentProd.stock}, solicitado: ${item.cantidad}"
                )
            }
            if (item.cantidad <= 0) {
                return@withContext ApiResponse.Error("La cantidad debe ser mayor a 0")
            }
        }

        try {
            val total = items.sumOf { it.producto.precioVenta * it.cantidad }
            val subtotal = total / 1.18
            val igv = total - subtotal
            val codigoVenta = "V${System.currentTimeMillis()}"

            val ventaId = ventaDao.insert(
                com.aplicaion.minimarketapp.db.entity.Venta(
                    codigoVenta = codigoVenta,
                    fecha = System.currentTimeMillis(),
                    subtotal = subtotal,
                    igv = igv,
                    total = total,
                    metodoPago = metodoPago
                )
            )

            // Insertar detalles y actualizar stock en tiempo real
            items.forEach { item ->
                detalleVentaDao.insert(
                    com.aplicaion.minimarketapp.db.entity.DetalleVenta(
                        ventaId = ventaId.toInt(),
                        productoId = item.producto.id,
                        cantidad = item.cantidad,
                        precioUnitario = item.producto.precioVenta,
                        subtotalLinea = item.producto.precioVenta * item.cantidad
                    )
                )
                val currentProd = productoDao.getByIdSync(item.producto.id)
                val nuevoStock = ((currentProd?.stock ?: item.producto.stock) - item.cantidad).toInt().coerceAtLeast(0)
                productoDao.updateStock(item.producto.id, nuevoStock)
            }

            ApiResponse.Success(ventaId, "Venta registrada con éxito")
        } catch (e: Exception) {
            ApiResponse.Error("Error al procesar la venta: ${e.localizedMessage}")
        }
    }

    // ==========================================
    // API IMPORT / EXPORT JSON
    // ==========================================

    suspend fun exportarBaseDatosJson(): ApiResponse<String> = withContext(Dispatchers.IO) {
        try {
            val json = JsonDatabaseManager.exportRoomToJson(db)
            ApiResponse.Success(json, "Base de datos exportada a JSON correctamente")
        } catch (e: Exception) {
            ApiResponse.Error("Error al exportar JSON: ${e.localizedMessage}")
        }
    }

    suspend fun restaurarBaseDatosDesdeJson(jsonString: String): ApiResponse<Int> = withContext(Dispatchers.IO) {
        try {
            val data = JsonDatabaseManager.parseJsonDatabase(jsonString)
            var count = 0

            data.proveedores.forEach {
                proveedorDao.insert(it)
            }
            data.categorias.forEach {
                categoriaDao.insert(it)
            }
            data.productos.forEach {
                val p = it.copy(
                    stock = it.stock.coerceAtLeast(0),
                    precioCompra = it.precioCompra.coerceAtLeast(0.0),
                    precioVenta = it.precioVenta.coerceAtLeast(0.01)
                )
                productoDao.insert(p)
                count++
            }
            ApiResponse.Success(count, "Se sincronizaron $count productos desde la Base de Datos JSON")
        } catch (e: Exception) {
            ApiResponse.Error("Error al restaurar JSON: ${e.localizedMessage}")
        }
    }
}
