package com.aplicaion.minimarketapp.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombreCompleto: String,
    val usuario: String,
    val correo: String,
    val contrasena: String,
    val rol: String = "VENDEDOR", // ADMIN / VENDEDOR
    val estado: String = "activo"
)
