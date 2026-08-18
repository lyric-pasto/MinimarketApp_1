package com.aplicaion.minimarketapp.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aplicaion.minimarketapp.db.entity.Venta
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(venta: Venta): Long

    @Update
    suspend fun update(venta: Venta)

    @Delete
    suspend fun delete(venta: Venta)

    @Query("UPDATE ventas SET estado = :estado WHERE id = :id")
    suspend fun updateEstado(id: Int, estado: String)

    @Query("DELETE FROM ventas WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM ventas")
    suspend fun deleteAll()

    @Query("SELECT * FROM ventas ORDER BY fecha DESC")
    fun getAll(): Flow<List<Venta>>

    @Query("SELECT * FROM ventas ORDER BY fecha DESC")
    suspend fun getAllList(): List<Venta>

    @Query("SELECT * FROM ventas WHERE fecha >= :inicio AND fecha <= :fin ORDER BY fecha DESC")
    fun getByFecha(inicio: Long, fin: Long): Flow<List<Venta>>

    @Query("SELECT * FROM ventas WHERE metodoPago = :metodoPago ORDER BY fecha DESC")
    fun getByMetodoPago(metodoPago: String): Flow<List<Venta>>

    @Query("SELECT * FROM ventas WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Venta?
}
