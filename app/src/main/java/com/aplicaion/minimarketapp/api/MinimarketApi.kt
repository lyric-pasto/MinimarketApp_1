package com.aplicaion.minimarketapp.api

import com.aplicaion.minimarketapp.db.entity.Categoria
import com.aplicaion.minimarketapp.db.entity.DetalleVenta
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.db.entity.Proveedor
import com.aplicaion.minimarketapp.db.entity.Usuario
import com.aplicaion.minimarketapp.db.entity.Venta
import com.aplicaion.minimarketapp.viewmodel.ItemCarrito
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de API unificado para el Sistema Minimarket.
 *
 * Arquitectura desacoplada:
 * La aplicación interactúa exclusivamente a través de esta interfaz.
 * Para cambiar de la Base de Datos Local/JSON a una API REST en la nube (Node.js, Python, Java, etc.),
 * solo se necesita reemplazar la implementación de MinimarketApi en MinimarketApiProvider.
 */
interface MinimarketApi {

    // --- PRODUCTOS ---
    fun getProductosFlow(): Flow<List<Producto>>
    suspend fun getProductos(): ApiResponse<List<Producto>>
    suspend fun getProductoPorId(id: Int): ApiResponse<Producto>
    suspend fun getProductoPorCodigo(codigo: String): ApiResponse<Producto>
    suspend fun guardarProducto(producto: Producto): ApiResponse<Long>
    suspend fun actualizarProducto(producto: Producto): ApiResponse<Unit>
    suspend fun eliminarProducto(producto: Producto): ApiResponse<Unit>

    // --- CATEGORÍAS ---
    fun getCategoriasFlow(): Flow<List<Categoria>>
    suspend fun getCategorias(): ApiResponse<List<Categoria>>

    // --- PROVEEDORES ---
    fun getProveedoresFlow(): Flow<List<Proveedor>>
    suspend fun getProveedores(): ApiResponse<List<Proveedor>>
    suspend fun getProveedorPorId(id: Int): ApiResponse<Proveedor>
    suspend fun guardarProveedor(proveedor: Proveedor): ApiResponse<Long>
    suspend fun actualizarProveedor(proveedor: Proveedor): ApiResponse<Unit>
    suspend fun eliminarProveedor(proveedor: Proveedor): ApiResponse<Unit>

    // --- USUARIOS Y AUTENTICACIÓN ---
    fun getUsuariosFlow(): Flow<List<Usuario>>
    suspend fun getUsuarios(): ApiResponse<List<Usuario>>
    suspend fun login(usuario: String, contrasena: String): ApiResponse<Usuario>
    suspend fun registrarUsuario(usuario: Usuario): ApiResponse<Long>
    suspend fun actualizarUsuario(usuario: Usuario): ApiResponse<Unit>
    suspend fun eliminarUsuario(usuario: Usuario): ApiResponse<Unit>
    suspend fun recuperarContrasena(usuario: String, correo: String, nuevaPass: String): ApiResponse<Unit>

    // --- VENTAS Y FACTURACIÓN ---
    fun getVentasFlow(): Flow<List<Venta>>
    fun getDetallesVentaFlow(ventaId: Int): Flow<List<DetalleVenta>>
    suspend fun registrarVenta(items: List<ItemCarrito>, metodoPago: String): ApiResponse<Venta>
    suspend fun inhabilitarVenta(ventaId: Int): ApiResponse<Unit>

    // --- BASE DE DATOS JSON (IMPORT / EXPORT / SYNC) ---
    suspend fun exportarBaseDatosJson(): ApiResponse<String>
    suspend fun importarBaseDatosJson(jsonString: String): ApiResponse<Int>
    suspend fun sincronizarConJsonLocal(): ApiResponse<Boolean>
}
