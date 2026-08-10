package com.aplicaion.minimarketapp.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aplicaion.minimarketapp.db.entity.Venta
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(venta: Venta): Long

    @Query("SELECT * FROM ventas ORDER BY fecha DESC")
    fun getAll(): Flow<List<Venta>>

    @Query("SELECT * FROM ventas WHERE fecha >= :inicio AND fecha <= :fin ORDER BY fecha DESC")
    fun getByFecha(inicio: Long, fin: Long): Flow<List<Venta>>

    @Query("SELECT * FROM ventas WHERE metodoPago = :metodoPago ORDER BY fecha DESC")
    fun getByMetodoPago(metodoPago: String): Flow<List<Venta>>

    @Query("SELECT * FROM ventas WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Venta?
}
