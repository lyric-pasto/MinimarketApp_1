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

        val subtotal = items.sumOf { it.producto.precioVenta * it.cantidad }
        val igv = subtotal * 0.18
        val total = subtotal + igv
        val codigoVenta = "V${System.currentTimeMillis()}"

        try {
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
                productoDao.updateStock(item.producto.id, item.producto.stock - item.cantidad)
            }

            return Result.success(ventaId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
