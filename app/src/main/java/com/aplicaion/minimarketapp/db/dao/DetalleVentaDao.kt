package com.aplicaion.minimarketapp.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aplicaion.minimarketapp.db.entity.DetalleVenta
import kotlinx.coroutines.flow.Flow

@Dao
interface DetalleVentaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detalle: DetalleVenta): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(detalles: List<DetalleVenta>)

    @Query("SELECT * FROM detalles_venta WHERE ventaId = :ventaId")
    fun getByVentaId(ventaId: Int): Flow<List<DetalleVenta>>

    @Query("SELECT * FROM detalles_venta WHERE ventaId = :ventaId")
    suspend fun getByVentaIdList(ventaId: Int): List<DetalleVenta>

    @Query("DELETE FROM detalles_venta WHERE ventaId = :ventaId")
    suspend fun deleteByVentaId(ventaId: Int)

    @Query("DELETE FROM detalles_venta")
    suspend fun deleteAll()
}
