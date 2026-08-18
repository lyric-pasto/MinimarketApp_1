package com.aplicaion.minimarketapp.repository

import com.aplicaion.minimarketapp.db.dao.DetalleVentaDao
import com.aplicaion.minimarketapp.db.dao.ProductoDao
import com.aplicaion.minimarketapp.db.dao.VentaDao
import com.aplicaion.minimarketapp.db.entity.DetalleVenta
import com.aplicaion.minimarketapp.db.entity.Venta
import com.aplicaion.minimarketapp.viewmodel.ItemCarrito
import kotlinx.coroutines.flow.Flow

class VentaRepository(
    private val ventaDao: VentaDao,
    private val detalleVentaDao: DetalleVentaDao,
    private val productoDao: ProductoDao
) {

    fun getAllVentas(): Flow<List<Venta>> = ventaDao.getAll()

    fun getVentasByFecha(inicio: Long, fin: Long): Flow<List<Venta>> =
        ventaDao.getByFecha(inicio, fin)

    fun getDetallesByVenta(ventaId: Int): Flow<List<DetalleVenta>> =
        detalleVentaDao.getByVentaId(ventaId)

    suspend fun registrarVenta(items: List<ItemCarrito>, metodoPago: String): Result<Long> {
        if (items.isEmpty()) {
            return Result.failure(Exception("El carrito está vacío"))
        }

        val total = items.sumOf { it.producto.precioVenta * it.cantidad }
        val subtotal = total / 1.18
        val igv = total - subtotal
        val codigoVenta = "V${System.currentTimeMillis()}"

        try {
            // Verificar stock antes de procesar la venta
            items.forEach { item ->
                val currentProd = productoDao.getByIdSync(item.producto.id)
                val unidadesEnterasRequeridas = Math.ceil(item.cantidad).toInt().coerceAtLeast(1)
                if (currentProd != null && currentProd.stock < unidadesEnterasRequeridas) {
                    return Result.failure(Exception("Stock insuficiente para '${item.producto.nombre}' (disponible: ${currentProd.stock})"))
                }
            }

            val ventaId = ventaDao.insert(
                Venta(
                    codigoVenta = codigoVenta,
                    fecha = System.currentTimeMillis(),
                    subtotal = subtotal,
                    igv = igv,
                    total = total,
                    metodoPago = metodoPago
                )
            )

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
                // Descontar stock
                val currentProd = productoDao.getByIdSync(item.producto.id)
                val unidadesADescontar = Math.ceil(item.cantidad).toInt().coerceAtLeast(1)
                val nuevoStock = ((currentProd?.stock ?: item.producto.stock) - unidadesADescontar).coerceAtLeast(0)
                productoDao.updateStock(item.producto.id, nuevoStock)
            }

            return Result.success(ventaId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun anularOEliminarVenta(ventaId: Int, restaurarStock: Boolean = true): Result<Boolean> {
        try {
            val venta = ventaDao.getById(ventaId) ?: return Result.failure(Exception("Venta no encontrada"))
            if (restaurarStock && venta.estado != "ANULADA" && venta.estado != "INHABILITADA") {
                val detalles = detalleVentaDao.getByVentaIdList(ventaId)
                detalles.forEach { d ->
                    val prod = productoDao.getByIdSync(d.productoId)
                    if (prod != null) {
                        val cantARestaurar = Math.ceil(d.cantidad).toInt().coerceAtLeast(1)
                        productoDao.updateStock(prod.id, prod.stock + cantARestaurar)
                    }
                }
            }
            detalleVentaDao.deleteByVentaId(ventaId)
            ventaDao.deleteById(ventaId)
            return Result.success(true)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun vaciarHistorialVentas(): Result<Boolean> {
        try {
            detalleVentaDao.deleteAll()
            ventaDao.deleteAll()
            return Result.success(true)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
