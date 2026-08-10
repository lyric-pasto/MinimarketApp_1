package com.aplicaion.minimarketapp.repository

import com.aplicaion.minimarketapp.db.dao.UsuarioDao
import com.aplicaion.minimarketapp.db.entity.Usuario
import com.aplicaion.minimarketapp.utils.Constants
import kotlinx.coroutines.flow.Flow

class AuthRepository(private val usuarioDao: UsuarioDao) {

    suspend fun login(usuario: String, contrasena: String): Usuario? {
        // Ensure default admin user exists if database was created
        if (usuarioDao.getCount() == 0) {
            val admin = Usuario(
                nombreCompleto = "Administrador Principal",
                usuario = "admin",
                correo = "admin@minimarket.com",
                contrasena = "admin123",
                rol = Constants.ROL_ADMIN,
                estado = Constants.ESTADO_ACTIVO
            )
            usuarioDao.insert(admin)
        }
        return usuarioDao.findByUsuarioYContrasena(usuario, contrasena)
    }

    suspend fun registrarUsuario(usuario: Usuario): Long {
        return usuarioDao.insert(usuario)
    }

    suspend fun existeUsuario(nombreUsuario: String): Boolean {
        return usuarioDao.getByUsuario(nombreUsuario) != null
    }

    fun getAllUsuarios(): Flow<List<Usuario>> = usuarioDao.getAll()

    suspend fun updateUsuario(usuario: Usuario) = usuarioDao.update(usuario)

    suspend fun deleteUsuario(usuario: Usuario) = usuarioDao.delete(usuario)
}
