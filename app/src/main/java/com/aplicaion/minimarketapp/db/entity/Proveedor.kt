package com.aplicaion.minimarketapp.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "proveedores",
    indices = [Index(value = ["ruc"], unique = true)]
)
data class Proveedor(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val celular: String = "",
    val direccion: String = "",
    val correo: String = "",
    val ruc: String,
    val estado: String = "activo",
    val fechaCreacion: Long = System.currentTimeMillis()
)
