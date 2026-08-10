package com.aplicaion.minimarketapp.repository

import com.aplicaion.minimarketapp.db.dao.CategoriaDao
import com.aplicaion.minimarketapp.db.entity.Categoria
import kotlinx.coroutines.flow.Flow

class CategoriaRepository(private val categoriaDao: CategoriaDao) {

    fun getAllCategorias(): Flow<List<Categoria>> = categoriaDao.getAll()

    suspend fun getAllCategoriasList(): List<Categoria> = categoriaDao.getAllList()

    suspend fun insertCategoria(categoria: Categoria): Long = categoriaDao.insert(categoria)

    suspend fun updateCategoria(categoria: Categoria) = categoriaDao.update(categoria)

    suspend fun deleteCategoria(categoria: Categoria) = categoriaDao.delete(categoria)
}
