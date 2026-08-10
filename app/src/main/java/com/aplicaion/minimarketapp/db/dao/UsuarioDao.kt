package com.aplicaion.minimarketapp.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aplicaion.minimarketapp.db.entity.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usuario: Usuario): Long

    @Query("SELECT * FROM usuarios WHERE (usuario = :usuario OR correo = :usuario) AND contrasena = :contrasena AND estado = 'activo' LIMIT 1")
    suspend fun findByUsuarioYContrasena(usuario: String, contrasena: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE usuario = :usuario LIMIT 1")
    suspend fun getByUsuario(usuario: String): Usuario?

    @Query("SELECT * FROM usuarios")
    fun getAll(): Flow<List<Usuario>>

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun getCount(): Int
}
