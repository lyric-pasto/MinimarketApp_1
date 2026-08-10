package com.aplicaion.minimarketapp.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ventas",
    indices = [Index(value = ["codigoVenta"], unique = true)]
)
data class Venta(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fecha: Long = System.currentTimeMillis(),
    val codigoVenta: String,
    val subtotal: Double,
    val igv: Double,
    val total: Double,
    val metodoPago: String = "EFECTIVO"
)
