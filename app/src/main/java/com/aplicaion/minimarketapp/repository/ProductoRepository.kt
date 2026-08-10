package com.aplicaion.minimarketapp.repository

import com.aplicaion.minimarketapp.db.dao.ProductoDao
import com.aplicaion.minimarketapp.db.entity.Producto
import kotlinx.coroutines.flow.Flow

class ProductoRepository(private val productoDao: ProductoDao) {

    fun getAllProductos(): Flow<List<Producto>> = productoDao.getAll()

    fun getProductosByCategoria(categoriaId: Int): Flow<List<Producto>> =
        productoDao.getByCategoria(categoriaId)

    fun searchProductos(query: String): Flow<List<Producto>> =
        productoDao.searchProductos(query)

    suspend fun getByCodigo(codigo: String): Producto? =
        productoDao.getByCodigo(codigo)

    suspend fun getById(id: Int): Producto? =
        productoDao.getById(id)

    fun getByIdLiveData(id: Int): androidx.lifecycle.LiveData<Producto> =
        productoDao.getByIdLiveData(id)

    suspend fun insertProducto(producto: Producto): Long =
        productoDao.insert(producto)

    suspend fun updateProducto(producto: Producto) =
        productoDao.update(producto)

    suspend fun actualizar(producto: Producto) =
        productoDao.actualizar(producto)

    suspend fun deleteProducto(producto: Producto) =
        productoDao.delete(producto)

    suspend fun updateStock(productoId: Int, cantidadDelta: Int) =
        productoDao.updateStock(productoId, cantidadDelta)
}
