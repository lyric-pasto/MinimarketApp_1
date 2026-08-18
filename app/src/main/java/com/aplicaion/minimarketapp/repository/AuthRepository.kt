package com.aplicaion.minimarketapp.repository

import com.aplicaion.minimarketapp.db.dao.UsuarioDao
import com.aplicaion.minimarketapp.db.entity.Usuario
import com.aplicaion.minimarketapp.utils.Constants
import kotlinx.coroutines.flow.Flow

class AuthRepository(private val usuarioDao: UsuarioDao) {

    suspend fun login(usuario: String, contrasena: String): Usuario? {
        val uTrim = usuario.trim()
        val pTrim = contrasena.trim()

        // Garantizar que existan los usuarios base del JSON si la tabla está vacía
        if (usuarioDao.getCount() == 0) {
            usuarioDao.insert(
                Usuario(
                    nombreCompleto = "Administrador Principal",
                    usuario = "admin",
                    correo = "admin@minimarket.com",
                    contrasena = "admin123",
                    rol = Constants.ROL_ADMIN,
                    estado = Constants.ESTADO_ACTIVO
                )
            )
            usuarioDao.insert(
                Usuario(
                    nombreCompleto = "Cajero Juan Perez",
                    usuario = "cajero1",
                    correo = "juan.cajero@minimarket.com",
                    contrasena = "cajero123",
                    rol = Constants.ROL_VENDEDOR,
                    estado = Constants.ESTADO_ACTIVO
                )
            )
        }

        var user = usuarioDao.findByUsuarioYContrasenaAnyStatus(uTrim, pTrim)

        // Respaldo de seguridad para credenciales por defecto si se limpiaron
        if (user == null) {
            if ((uTrim.equals("admin", ignoreCase = true) || uTrim.equals("admin@minimarket.com", ignoreCase = true)) && pTrim == "admin123") {
                val defaultAdmin = Usuario(
                    nombreCompleto = "Administrador Principal",
                    usuario = "admin",
                    correo = "admin@minimarket.com",
                    contrasena = "admin123",
                    rol = Constants.ROL_ADMIN,
                    estado = Constants.ESTADO_ACTIVO
                )
                usuarioDao.insert(defaultAdmin)
                user = defaultAdmin
            } else if ((uTrim.equals("cajero1", ignoreCase = true) || uTrim.equals("juan.cajero@minimarket.com", ignoreCase = true)) && pTrim == "cajero123") {
                val defaultCajero = Usuario(
                    nombreCompleto = "Cajero Juan Perez",
                    usuario = "cajero1",
                    correo = "juan.cajero@minimarket.com",
                    contrasena = "cajero123",
                    rol = Constants.ROL_VENDEDOR,
                    estado = Constants.ESTADO_ACTIVO
                )
                usuarioDao.insert(defaultCajero)
                user = defaultCajero
            }
        }

        if (user == null) return null

        if (user.estado.equals(Constants.ESTADO_INACTIVO, ignoreCase = true)) {
            throw IllegalStateException("Tu cuenta ha sido inhabilitada. Contacta al Administrador.")
        }
        if (user.estado.equals(Constants.ESTADO_PENDIENTE, ignoreCase = true)) {
            throw IllegalStateException("Tu solicitud de registro está pendiente de aprobación por el Administrador.")
        }
        return user
    }

    suspend fun registrarUsuario(usuario: Usuario): Long {
        return usuarioDao.insert(usuario)
    }

    suspend fun existeUsuario(nombreUsuario: String, excludeId: Int = 0): Boolean {
        return if (excludeId > 0) {
            usuarioDao.getByUsuarioExcludingId(nombreUsuario.trim(), excludeId) != null
        } else {
            usuarioDao.getByUsuario(nombreUsuario.trim()) != null
        }
    }

    suspend fun existeCorreo(correo: String, excludeId: Int = 0): Boolean {
        return if (excludeId > 0) {
            usuarioDao.getByCorreoExcludingId(correo.trim(), excludeId) != null
        } else {
            usuarioDao.getByCorreo(correo.trim()) != null
        }
    }

    fun getAllUsuarios(): Flow<List<Usuario>> = usuarioDao.getAll()

    suspend fun updateUsuario(usuario: Usuario) = usuarioDao.update(usuario)

    suspend fun deleteUsuario(usuario: Usuario) = usuarioDao.delete(usuario)
}
