package com.aplicaion.minimarketapp.repository

import com.aplicaion.minimarketapp.db.dao.VentaDao
import com.aplicaion.minimarketapp.db.entity.Venta
import kotlinx.coroutines.flow.Flow

class ReporteRepository(private val ventaDao: VentaDao) {

    fun getVentasFlow(): Flow<List<Venta>> = ventaDao.getAll()

    fun getVentasByFecha(inicio: Long, fin: Long): Flow<List<Venta>> =
        ventaDao.getByFecha(inicio, fin)
}
