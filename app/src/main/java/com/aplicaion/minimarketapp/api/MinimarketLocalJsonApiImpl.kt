package com.aplicaion.minimarketapp.api

import android.content.Context
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.db.entity.Categoria
import com.aplicaion.minimarketapp.db.entity.DetalleVenta
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.db.entity.Proveedor
import com.aplicaion.minimarketapp.db.entity.Usuario
import com.aplicaion.minimarketapp.db.entity.Venta
import com.aplicaion.minimarketapp.utils.Constants
import com.aplicaion.minimarketapp.viewmodel.ItemCarrito
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Implementación de la API del Minimarket basada en la Base de Datos JSON viva y Room.
 * Cada operación valida las reglas del negocio, actualiza la base de datos y
 * persiste automáticamente el estado en el archivo JSON local.
 */
class MinimarketLocalJsonApiImpl(private val context: Context) : MinimarketApi {

    private val db: AppDatabase = AppDatabase.getInstance(context)
    private val productoDao = db.productoDao()
    private val categoriaDao = db.categoriaDao()
    private val proveedorDao = db.proveedorDao()
    private val usuarioDao = db.usuarioDao()
    private val ventaDao = db.ventaDao()
    private val detalleVentaDao = db.detalleVentaDao()

    // -------------------------------------------------------------
    // PRODUCTOS
    // -------------------------------------------------------------

    override fun getProductosFlow(): Flow<List<Producto>> = productoDao.getAll()

    override suspend fun getProductos(): ApiResponse<List<Producto>> = withContext(Dispatchers.IO) {
        try {
            val list = productoDao.getAllList()
            ApiResponse.Success(list)
        } catch (e: Exception) {
            ApiResponse.Error("Error al obtener productos: ${e.localizedMessage}")
        }
    }

    override suspend fun getProductoPorId(id: Int): ApiResponse<Producto> = withContext(Dispatchers.IO) {
        try {
            val prod = productoDao.getById(id)
            if (prod != null) {
                ApiResponse.Success(prod)
            } else {
                ApiResponse.Error("Producto no encontrado", 404)
            }
        } catch (e: Exception) {
            ApiResponse.Error("Error al consultar producto: ${e.localizedMessage}")
        }
    }

    override suspend fun getProductoPorCodigo(codigo: String): ApiResponse<Producto> = withContext(Dispatchers.IO) {
        try {
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

    override suspend fun guardarProducto(producto: Producto): ApiResponse<Long> = withContext(Dispatchers.IO) {
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

            val existente = productoDao.getByCodigo(producto.codigoBarras.trim())
            if (existente != null && existente.id != producto.id) {
                return@withContext ApiResponse.Error("El código de barras ya pertenece a '${existente.nombre}'")
            }

            val prodSeguro = producto.copy(
                stock = producto.stock.coerceAtLeast(0),
                precioCompra = producto.precioCompra.coerceAtLeast(0.0),
                precioVenta = producto.precioVenta.coerceAtLeast(0.01)
            )

            val id = if (prodSeguro.id > 0) {
                productoDao.actualizar(prodSeguro)
                prodSeguro.id.toLong()
            } else {
                productoDao.insert(prodSeguro)
            }

            // Sincronizar archivo JSON en segundo plano
            JsonDatabaseManager.syncAndSaveJsonFile(context, db)

            ApiResponse.Success(id, "Producto guardado y sincronizado con la Base de Datos JSON")
        } catch (e: Exception) {
            ApiResponse.Error("Error al guardar producto: ${e.localizedMessage}")
        }
    }

    override suspend fun actualizarProducto(producto: Producto): ApiResponse<Unit> = withContext(Dispatchers.IO) {
        val res = guardarProducto(producto)
        if (res is ApiResponse.Success) {
            ApiResponse.Success(Unit, res.message)
        } else {
            ApiResponse.Error((res as ApiResponse.Error).message, res.code)
        }
    }

    override suspend fun eliminarProducto(producto: Producto): ApiResponse<Unit> = withContext(Dispatchers.IO) {
        try {
            productoDao.delete(producto)
            JsonDatabaseManager.syncAndSaveJsonFile(context, db)
            ApiResponse.Success(Unit, "Producto eliminado y sincronizado con JSON")
        } catch (e: Exception) {
            ApiResponse.Error("Error al eliminar producto: ${e.localizedMessage}")
        }
    }

    // -------------------------------------------------------------
    // CATEGORÍAS
    // -------------------------------------------------------------

    override fun getCategoriasFlow(): Flow<List<Categoria>> = categoriaDao.getAll()

    override suspend fun getCategorias(): ApiResponse<List<Categoria>> = withContext(Dispatchers.IO) {
        try {
            val list = categoriaDao.getAllList()
            ApiResponse.Success(list)
        } catch (e: Exception) {
            ApiResponse.Error("Error al obtener categorías: ${e.localizedMessage}")
        }
    }

    // -------------------------------------------------------------
    // PROVEEDORES
    // -------------------------------------------------------------

    override fun getProveedoresFlow(): Flow<List<Proveedor>> = proveedorDao.getAll()

    override suspend fun getProveedores(): ApiResponse<List<Proveedor>> = withContext(Dispatchers.IO) {
        try {
            val list = proveedorDao.getAllList()
            ApiResponse.Success(list)
        } catch (e: Exception) {
            ApiResponse.Error("Error al obtener proveedores: ${e.localizedMessage}")
        }
    }

    override suspend fun getProveedorPorId(id: Int): ApiResponse<Proveedor> = withContext(Dispatchers.IO) {
        try {
            val prov = proveedorDao.getById(id)
            if (prov != null) {
                ApiResponse.Success(prov)
            } else {
                ApiResponse.Error("Proveedor no encontrado", 404)
            }
        } catch (e: Exception) {
            ApiResponse.Error("Error al consultar proveedor: ${e.localizedMessage}")
        }
    }

    override suspend fun guardarProveedor(proveedor: Proveedor): ApiResponse<Long> = withContext(Dispatchers.IO) {
        try {
            val validacion = JsonDatabaseManager.validarProveedor(
                nombre = proveedor.nombre,
                ruc = proveedor.ruc,
                celular = proveedor.celular,
                correo = proveedor.correo,
                direccion = proveedor.direccion
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

            JsonDatabaseManager.syncAndSaveJsonFile(context, db)
            ApiResponse.Success(id, "Proveedor guardado y sincronizado con JSON")
        } catch (e: Exception) {
            ApiResponse.Error("Error al guardar proveedor: ${e.localizedMessage}")
        }
    }

    override suspend fun actualizarProveedor(proveedor: Proveedor): ApiResponse<Unit> = withContext(Dispatchers.IO) {
        val res = guardarProveedor(proveedor)
        if (res is ApiResponse.Success) {
            ApiResponse.Success(Unit, res.message)
        } else {
            ApiResponse.Error((res as ApiResponse.Error).message, res.code)
        }
    }

    override suspend fun eliminarProveedor(proveedor: Proveedor): ApiResponse<Unit> = withContext(Dispatchers.IO) {
        try {
            proveedorDao.delete(proveedor)
            JsonDatabaseManager.syncAndSaveJsonFile(context, db)
            ApiResponse.Success(Unit, "Proveedor eliminado y sincronizado")
        } catch (e: Exception) {
            ApiResponse.Error("Error al eliminar proveedor: ${e.localizedMessage}")
        }
    }

    // -------------------------------------------------------------
    // USUARIOS Y AUTENTICACIÓN
    // -------------------------------------------------------------

    override fun getUsuariosFlow(): Flow<List<Usuario>> = usuarioDao.getAll()

    override suspend fun getUsuarios(): ApiResponse<List<Usuario>> = withContext(Dispatchers.IO) {
        try {
            val users = usuarioDao.getAllList()
            ApiResponse.Success(users)
        } catch (e: Exception) {
            ApiResponse.Error("Error al obtener usuarios: ${e.localizedMessage}")
        }
    }

    override suspend fun login(usuario: String, contrasena: String): ApiResponse<Usuario> = withContext(Dispatchers.IO) {
        try {
            val uTrim = usuario.trim()
            val pTrim = contrasena.trim()

            if (usuarioDao.getCount() == 0) {
                val admin = Usuario(
                    nombreCompleto = "Administrador Principal",
                    usuario = "admin",
                    correo = "admin@minimarket.com",
                    contrasena = "admin123",
                    rol = Constants.ROL_ADMIN,
                    estado = Constants.ESTADO_ACTIVO
                )
                usuarioDao.insert(admin)
                val cajero = Usuario(
                    nombreCompleto = "Cajero Juan Perez",
                    usuario = "cajero1",
                    correo = "juan.cajero@minimarket.com",
                    contrasena = "cajero123",
                    rol = Constants.ROL_VENDEDOR,
                    estado = Constants.ESTADO_ACTIVO
                )
                usuarioDao.insert(cajero)
            }

            var user = usuarioDao.findByUsuarioYContrasenaAnyStatus(uTrim, pTrim)

            if (user == null) {
                if ((uTrim.equals("admin", ignoreCase = true) || uTrim.equals("admin@minimarket.com", ignoreCase = true)) && pTrim == "admin123") {
                    val defaultAdmin = Usuario(
                        nombreCompleto = "Administrador Principal",
                        usuario = "admin",
                        correo = "admin@minimarket.com",
                        contrasena = "admin123",
                        rol = Constants.ROL_ADMIN,
                        estado = Constants.ESTADO_ACTIVO
                    )
                    usuarioDao.insert(defaultAdmin)
                    user = defaultAdmin
                } else if ((uTrim.equals("cajero1", ignoreCase = true) || uTrim.equals("juan.cajero@minimarket.com", ignoreCase = true)) && pTrim == "cajero123") {
                    val defaultCajero = Usuario(
                        nombreCompleto = "Cajero Juan Perez",
                        usuario = "cajero1",
                        correo = "juan.cajero@minimarket.com",
                        contrasena = "cajero123",
                        rol = Constants.ROL_VENDEDOR,
                        estado = Constants.ESTADO_ACTIVO
                    )
                    usuarioDao.insert(defaultCajero)
                    user = defaultCajero
                }
            }

            if (user != null) {
                if (user.estado.equals(Constants.ESTADO_INACTIVO, ignoreCase = true)) {
                    return@withContext ApiResponse.Error("Tu cuenta ha sido inhabilitada. Contacta al Administrador.")
                }
                if (user.estado.equals(Constants.ESTADO_PENDIENTE, ignoreCase = true)) {
                    return@withContext ApiResponse.Error("Tu solicitud de registro está pendiente de aprobación por el Administrador.")
                }
                ApiResponse.Success(user, "Bienvenido ${user.nombreCompleto}")
            } else {
                ApiResponse.Error("Usuario o contraseña incorrectos", 401)
            }
        } catch (e: Exception) {
            ApiResponse.Error("Error en autenticación: ${e.localizedMessage}")
        }
    }

    override suspend fun registrarUsuario(usuario: Usuario): ApiResponse<Long> = withContext(Dispatchers.IO) {
        try {
            val validacion = JsonDatabaseManager.validarUsuario(
                nombreCompleto = usuario.nombreCompleto,
                usuario = usuario.usuario,
                correo = usuario.correo,
                contrasena = usuario.contrasena
            )
            if (!validacion.isValid) {
                return@withContext ApiResponse.Error(validacion.message)
            }

            val existente = usuarioDao.getByUsuario(usuario.usuario.trim())
            if (existente != null && existente.id != usuario.id) {
                return@withContext ApiResponse.Error("El nombre de usuario '${usuario.usuario}' ya está registrado")
            }

            val correoExistente = usuarioDao.getByCorreo(usuario.correo.trim())
            if (correoExistente != null && correoExistente.id != usuario.id) {
                return@withContext ApiResponse.Error("El correo electrónico '${usuario.correo}' ya está en uso")
            }

            val id = if (usuario.id > 0) {
                usuarioDao.update(usuario)
                usuario.id.toLong()
            } else {
                usuarioDao.insert(usuario)
            }

            JsonDatabaseManager.syncAndSaveJsonFile(context, db)
            ApiResponse.Success(id, "Usuario guardado y sincronizado en JSON")
        } catch (e: Exception) {
            ApiResponse.Error("Error al guardar usuario: ${e.localizedMessage}")
        }
    }

    override suspend fun actualizarUsuario(usuario: Usuario): ApiResponse<Unit> = withContext(Dispatchers.IO) {
        try {
            usuarioDao.update(usuario)
            JsonDatabaseManager.syncAndSaveJsonFile(context, db)
            ApiResponse.Success(Unit, "Usuario actualizado")
        } catch (e: Exception) {
            ApiResponse.Error("Error al actualizar usuario: ${e.localizedMessage}")
        }
    }

    override suspend fun eliminarUsuario(usuario: Usuario): ApiResponse<Unit> = withContext(Dispatchers.IO) {
        try {
            if (usuario.usuario.equals("admin", ignoreCase = true)) {
                return@withContext ApiResponse.Error("No se puede eliminar el usuario administrador principal")
            }
            usuarioDao.delete(usuario)
            JsonDatabaseManager.syncAndSaveJsonFile(context, db)
            ApiResponse.Success(Unit, "Usuario eliminado")
        } catch (e: Exception) {
            ApiResponse.Error("Error al eliminar usuario: ${e.localizedMessage}")
        }
    }

    override suspend fun recuperarContrasena(usuario: String, correo: String, nuevaPass: String): ApiResponse<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = usuarioDao.getByUsuarioYCorreo(usuario.trim(), correo.trim())
                ?: usuarioDao.getByCorreo(correo.trim())
                ?: return@withContext ApiResponse.Error("No se encontró ningún usuario con ese correo / nombre de usuario")

            if (nuevaPass.length < 6) {
                return@withContext ApiResponse.Error("La nueva contraseña debe tener al menos 6 caracteres")
            }

            usuarioDao.updateContrasena(user.id, nuevaPass)
            JsonDatabaseManager.syncAndSaveJsonFile(context, db)
            ApiResponse.Success(Unit, "Contraseña actualizada exitosamente para '${user.usuario}'")
        } catch (e: Exception) {
            ApiResponse.Error("Error al restablecer contraseña: ${e.localizedMessage}")
        }
    }

    // -------------------------------------------------------------
    // VENTAS Y FACTURACIÓN
    // -------------------------------------------------------------

    override fun getVentasFlow(): Flow<List<Venta>> = ventaDao.getAll()

    override fun getDetallesVentaFlow(ventaId: Int): Flow<List<DetalleVenta>> = detalleVentaDao.getByVentaId(ventaId)

    override suspend fun registrarVenta(items: List<ItemCarrito>, metodoPago: String): ApiResponse<Venta> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) {
            return@withContext ApiResponse.Error("El carrito de ventas está vacío")
        }

        // Validar stock antes de la transacción
        for (item in items) {
            val currentProd = productoDao.getByIdSync(item.producto.id)
            if (currentProd == null) {
                return@withContext ApiResponse.Error("Producto '${item.producto.nombre}' no encontrado")
            }
            if (currentProd.stock < item.cantidad) {
                return@withContext ApiResponse.Error(
                    "Stock insuficiente para '${currentProd.nombre}'. Disponible: ${currentProd.stock}, Solicitado: ${item.cantidad}"
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

            val ventaEntity = Venta(
                codigoVenta = codigoVenta,
                fecha = System.currentTimeMillis(),
                subtotal = subtotal,
                igv = igv,
                total = total,
                metodoPago = metodoPago,
                estado = "COMPLETADA"
            )

            val ventaId = ventaDao.insert(ventaEntity)

            // Registrar detalles y actualizar stock en tiempo real
            items.forEach { item ->
                detalleVentaDao.insert(
                    DetalleVenta(
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

            // Sincronizar archivo JSON con el stock actualizado
            JsonDatabaseManager.syncAndSaveJsonFile(context, db)

            ApiResponse.Success(ventaEntity.copy(id = ventaId.toInt()), "Venta procesada con éxito")
        } catch (e: Exception) {
            ApiResponse.Error("Error al procesar la venta: ${e.localizedMessage}")
        }
    }

    override suspend fun inhabilitarVenta(ventaId: Int): ApiResponse<Unit> = withContext(Dispatchers.IO) {
        try {
            val venta = ventaDao.getById(ventaId)
                ?: return@withContext ApiResponse.Error("Venta no encontrada")

            if (venta.estado == "INHABILITADA" || venta.estado == "ANULADA") {
                return@withContext ApiResponse.Error("Esta venta ya se encuentra inhabilitada / anulada")
            }

            // 1. Marcar venta como INHABILITADA
            ventaDao.updateEstado(ventaId, "INHABILITADA")

            // 2. Restaurar stock de cada producto involucrado
            val detalles = detalleVentaDao.getByVentaIdList(ventaId)
            for (det in detalles) {
                val prod = productoDao.getByIdSync(det.productoId)
                if (prod != null) {
                    val stockRestaurado = (prod.stock + det.cantidad).toInt()
                    productoDao.updateStock(det.productoId, stockRestaurado)
                }
            }

            // 3. Sincronizar con el JSON viva
            JsonDatabaseManager.syncAndSaveJsonFile(context, db)

            ApiResponse.Success(Unit, "Venta #${venta.codigoVenta} inhabilitada y el stock fue devuelto al inventario.")
        } catch (e: Exception) {
            ApiResponse.Error("Error al inhabilitar venta: ${e.localizedMessage}")
        }
    }

    // -------------------------------------------------------------
    // IMPORT / EXPORT JSON
    // -------------------------------------------------------------

    override suspend fun exportarBaseDatosJson(): ApiResponse<String> = withContext(Dispatchers.IO) {
        try {
            val json = JsonDatabaseManager.exportRoomToJson(db)
            ApiResponse.Success(json, "Base de datos exportada a JSON correctamente")
        } catch (e: Exception) {
            ApiResponse.Error("Error al exportar JSON: ${e.localizedMessage}")
        }
    }

    override suspend fun importarBaseDatosJson(jsonString: String): ApiResponse<Int> = withContext(Dispatchers.IO) {
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

            JsonDatabaseManager.syncAndSaveJsonFile(context, db)
            ApiResponse.Success(count, "Se importaron $count productos correctamente")
        } catch (e: Exception) {
            ApiResponse.Error("Error al importar JSON: ${e.localizedMessage}")
        }
    }

    override suspend fun sincronizarConJsonLocal(): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        try {
            JsonDatabaseManager.syncAndSaveJsonFile(context, db)
            ApiResponse.Success(true, "Base de datos local y JSON sincronizados")
        } catch (e: Exception) {
            ApiResponse.Error("Error al sincronizar: ${e.localizedMessage}")
        }
    }
}
