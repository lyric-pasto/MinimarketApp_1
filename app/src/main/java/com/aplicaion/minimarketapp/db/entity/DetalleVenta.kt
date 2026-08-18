package com.aplicaion.minimarketapp.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "detalles_venta",
    indices = [
        Index("ventaId"),
        Index("productoId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = Venta::class,
            parentColumns = ["id"],
            childColumns = ["ventaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class DetalleVenta(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val ventaId: Int,
    val productoId: Int,
    val cantidad: Double = 1.0,
    val precioUnitario: Double,
    val subtotalLinea: Double
)
