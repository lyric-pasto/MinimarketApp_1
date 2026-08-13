package com.aplicaion.minimarketapp.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aplicaion.minimarketapp.db.entity.Proveedor
import kotlinx.coroutines.flow.Flow

@Dao
interface ProveedorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(proveedor: Proveedor): Long

    @Update
    suspend fun update(proveedor: Proveedor)

    @Delete
    suspend fun delete(proveedor: Proveedor)

    @Query("SELECT * FROM proveedores ORDER BY nombre ASC")
    fun getAll(): Flow<List<Proveedor>>

    @Query("SELECT * FROM proveedores ORDER BY nombre ASC")
    suspend fun getAllList(): List<Proveedor>

    @Query("SELECT * FROM proveedores WHERE ruc = :ruc LIMIT 1")
    suspend fun getByRuc(ruc: String): Proveedor?

    @Query("SELECT * FROM proveedores WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Proveedor?
}
