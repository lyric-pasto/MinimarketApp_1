package com.aplicaion.minimarketapp.repository

import com.aplicaion.minimarketapp.db.dao.DetalleVentaDao
import com.aplicaion.minimarketapp.db.dao.ProductoDao
import com.aplicaion.minimarketapp.db.dao.VentaDao
import com.aplicaion.minimarketapp.db.entity.DetalleVenta
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.db.entity.Venta
import kotlinx.coroutines.flow.Flow

data class ItemCarrito(
    val producto: Producto,
    var cantidad: Int
) {
    val subtotalLinea: Double
        get() = producto.precioVenta * cantidad
}

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

    suspend fun registrarVenta(
        cartItems: List<ItemCarrito>,
        metodoPago: String
    ): Result<Long> {
        if (cartItems.isEmpty()) {
            return Result.failure(Exception("El carrito está vacío"))
        }

        // Validate stock for all items
        for (item in cartItems) {
            val producto = productoDao.getById(item.producto.id)
                ?: return Result.failure(Exception("Producto '${item.producto.nombre}' no encontrado"))

            if (producto.stock < item.cantidad) {
                return Result.failure(Exception("Stock insuficiente para '${producto.nombre}'. Disponible: ${producto.stock}"))
            }
        }

        // Calculate totals
        val subtotalTotal = cartItems.sumOf { it.subtotalLinea }
        val igvTotal = Math.round(subtotalTotal * 0.18 * 100.0) / 100.0
        val totalFinal = Math.round((subtotalTotal + igvTotal) * 100.0) / 100.0
        val codigoVenta = "VNT-${System.currentTimeMillis()}"

        val venta = Venta(
            codigoVenta = codigoVenta,
            subtotal = subtotalTotal,
            igv = igvTotal,
            total = totalFinal,
            metodoPago = metodoPago,
            fecha = System.currentTimeMillis()
        )

        val ventaId = ventaDao.insert(venta)

        // Insert details & discount stock automatically
        for (item in cartItems) {
            val detalle = DetalleVenta(
                ventaId = ventaId.toInt(),
                productoId = item.producto.id,
                cantidad = item.cantidad,
                precioUnitario = item.producto.precioVenta,
                subtotalLinea = item.subtotalLinea
            )
            detalleVentaDao.insert(detalle)

            // Subtract stock
            productoDao.updateStock(item.producto.id, -item.cantidad)
        }

        return Result.success(ventaId)
    }
}
