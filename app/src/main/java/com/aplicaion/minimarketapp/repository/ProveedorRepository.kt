package com.aplicaion.minimarketapp.repository

import com.aplicaion.minimarketapp.db.dao.ProveedorDao
import com.aplicaion.minimarketapp.db.entity.Proveedor
import kotlinx.coroutines.flow.Flow

class ProveedorRepository(private val proveedorDao: ProveedorDao) {

    fun getAllProveedores(): Flow<List<Proveedor>> = proveedorDao.getAll()

    suspend fun getAllProveedoresList(): List<Proveedor> = proveedorDao.getAllList()

    suspend fun getByRuc(ruc: String): Proveedor? = proveedorDao.getByRuc(ruc)

    suspend fun insertProveedor(proveedor: Proveedor): Long = proveedorDao.insert(proveedor)

    suspend fun updateProveedor(proveedor: Proveedor) = proveedorDao.update(proveedor)

    suspend fun deleteProveedor(proveedor: Proveedor) = proveedorDao.delete(proveedor)
}
