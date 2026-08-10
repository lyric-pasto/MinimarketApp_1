package com.aplicaion.minimarketapp.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aplicaion.minimarketapp.db.entity.Producto
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(producto: Producto): Long

    @Update
    suspend fun update(producto: Producto)

    @Delete
    suspend fun delete(producto: Producto)

    @Query("SELECT * FROM productos ORDER BY nombre ASC")
    fun getAll(): Flow<List<Producto>>

    @Query("SELECT * FROM productos WHERE categoriaId = :categoriaId ORDER BY nombre ASC")
    fun getByCategoria(categoriaId: Int): Flow<List<Producto>>

    @Query("SELECT * FROM productos WHERE codigoBarras = :codigo LIMIT 1")
    suspend fun getByCodigo(codigo: String): Producto?

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Producto?

    @Query("UPDATE productos SET stock = stock + :cantidadDelta WHERE id = :productoId")
    suspend fun updateStock(productoId: Int, cantidadDelta: Int)

    @Query("SELECT * FROM productos WHERE nombre LIKE '%' || :query || '%' OR codigoBarras LIKE '%' || :query || '%' ORDER BY nombre ASC")
    fun searchProductos(query: String): Flow<List<Producto>>

    @Query("SELECT COUNT(*) FROM productos")
    suspend fun getCount(): Int
}
