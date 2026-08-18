package com.aplicaion.minimarketapp.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aplicaion.minimarketapp.db.entity.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usuario: Usuario): Long

    @Update
    suspend fun update(usuario: Usuario)

    @Delete
    suspend fun delete(usuario: Usuario)

    @Query("SELECT * FROM usuarios WHERE (LOWER(TRIM(usuario)) = LOWER(TRIM(:usuario)) OR LOWER(TRIM(correo)) = LOWER(TRIM(:usuario))) AND TRIM(contrasena) = TRIM(:contrasena) AND LOWER(estado) = 'activo' LIMIT 1")
    suspend fun findByUsuarioYContrasena(usuario: String, contrasena: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE (LOWER(TRIM(usuario)) = LOWER(TRIM(:usuario)) OR LOWER(TRIM(correo)) = LOWER(TRIM(:usuario))) AND TRIM(contrasena) = TRIM(:contrasena) LIMIT 1")
    suspend fun findByUsuarioYContrasenaAnyStatus(usuario: String, contrasena: String): Usuario?

    @Query("UPDATE usuarios SET estado = :nuevoEstado WHERE id = :id")
    suspend fun updateEstado(id: Int, nuevoEstado: String)

    @Query("SELECT * FROM usuarios WHERE LOWER(TRIM(usuario)) = LOWER(TRIM(:usuario)) LIMIT 1")
    suspend fun getByUsuario(usuario: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE LOWER(TRIM(correo)) = LOWER(TRIM(:correo)) LIMIT 1")
    suspend fun getByCorreo(correo: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE LOWER(TRIM(usuario)) = LOWER(TRIM(:usuario)) AND id != :excludeId LIMIT 1")
    suspend fun getByUsuarioExcludingId(usuario: String, excludeId: Int): Usuario?

    @Query("SELECT * FROM usuarios WHERE LOWER(TRIM(correo)) = LOWER(TRIM(:correo)) AND id != :excludeId LIMIT 1")
    suspend fun getByCorreoExcludingId(correo: String, excludeId: Int): Usuario?

    @Query("SELECT * FROM usuarios WHERE (LOWER(TRIM(usuario)) = LOWER(TRIM(:usuario)) OR LOWER(TRIM(correo)) = LOWER(TRIM(:correo))) LIMIT 1")
    suspend fun getByUsuarioYCorreo(usuario: String, correo: String): Usuario?

    @Query("UPDATE usuarios SET contrasena = :nuevaPass WHERE id = :id")
    suspend fun updateContrasena(id: Int, nuevaPass: String)

    @Query("SELECT * FROM usuarios")
    fun getAll(): Flow<List<Usuario>>

    @Query("SELECT * FROM usuarios")
    suspend fun getAllList(): List<Usuario>

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun getCount(): Int
}
