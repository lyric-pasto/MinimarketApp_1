package com.aplicaion.minimarketapp.utils

import java.text.NumberFormat
import java.util.Locale

fun Double.formatSoles(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "PE"))
    return format.format(this).replace("PEN", "S/").replace("PE", "")
}

fun Double.round2(): Double {
    return Math.round(this * 100.0) / 100.0
}
