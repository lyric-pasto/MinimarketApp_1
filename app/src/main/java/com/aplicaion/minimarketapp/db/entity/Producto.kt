package com.aplicaion.minimarketapp.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "productos",
    indices = [Index(value = ["codigoBarras"], unique = true)]
)
data class Producto(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val categoriaId: Int,
    val precioCompra: Double,
    val precioVenta: Double,
    val stock: Int,
    val descripcion: String = "",
    val codigoBarras: String,
    val proveedorId: Int? = null,
    val imagenPath: String? = null,
    val esPorPeso: Boolean = false,
    val unidadMedida: String = "UND", // "UND", "KG", "PAQ", "G"
    val tipoVenta: String = "UNIDAD" // "UNIDAD", "PESO"
)
