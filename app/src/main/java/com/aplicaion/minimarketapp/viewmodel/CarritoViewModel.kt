package com.aplicaion.minimarketapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aplicaion.minimarketapp.db.entity.Producto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ItemCarrito(
    val producto: Producto,
    val cantidad: Double = 1.0,
    val etiquetaPeso: String? = null
) {
    val subtotalLinea: Double
        get() = producto.precioVenta * cantidad

    val cantidadFormateada: String
        get() {
            return if (producto.esPorPeso || producto.tipoVenta == "PESO") {
                if (cantidad == 0.25) "1/4 kg (0.25 kg)"
                else if (cantidad == 0.50) "1/2 kg (0.50 kg)"
                else if (cantidad == 0.75) "3/4 kg (0.75 kg)"
                else if (cantidad == 1.00) "1.00 kg"
                else String.format(java.util.Locale.US, "%.2f kg", cantidad)
            } else {
                if (cantidad % 1.0 == 0.0) "${cantidad.toInt()} und"
                else String.format(java.util.Locale.US, "%.2f und", cantidad)
            }
        }
}

class CarritoViewModel : ViewModel() {

    private val _items = _sharedItems
    val items: StateFlow<List<ItemCarrito>> = _items

    val totalItems: StateFlow<Int> = _items.map { list ->
        list.size
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val total: StateFlow<Double> = _items.map { list ->
        list.sumOf { it.producto.precioVenta * it.cantidad }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val subtotal: StateFlow<Double> = _items.map { list ->
        val tot = list.sumOf { it.producto.precioVenta * it.cantidad }
        tot / 1.18
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val igv: StateFlow<Double> = _items.map { list ->
        val tot = list.sumOf { it.producto.precioVenta * it.cantidad }
        tot - (tot / 1.18)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    fun agregar(producto: Producto, cantidad: Double = 1.0, etiquetaPeso: String? = null): Boolean {
        if (producto.stock <= 0) return false
        val lista = _sharedItems.value.toMutableList()
        val index = lista.indexOfFirst { it.producto.id == producto.id && it.etiquetaPeso == etiquetaPeso }
        if (index != -1) {
            val actual = lista[index]
            val nuevaCant = actual.cantidad + cantidad
            if (nuevaCant > producto.stock) {
                return false
            }
            lista[index] = actual.copy(cantidad = nuevaCant)
        } else {
            lista.add(ItemCarrito(producto, cantidad, etiquetaPeso))
        }
        _sharedItems.value = lista.toList()
        return true
    }

    fun aumentarPorId(productoId: Int): Boolean {
        val lista = _sharedItems.value.toMutableList()
        val index = lista.indexOfFirst { it.producto.id == productoId }
        if (index != -1) {
            val actual = lista[index]
            val paso = if (actual.producto.esPorPeso || actual.producto.tipoVenta == "PESO") 0.25 else 1.0
            val nuevaCant = actual.cantidad + paso
            if (nuevaCant > actual.producto.stock) {
                return false
            }
            lista[index] = actual.copy(cantidad = nuevaCant)
            _sharedItems.value = lista.toList()
            return true
        }
        return false
    }

    fun reducir(producto: Producto) {
        reducirPorId(producto.id)
    }

    fun reducirPorId(productoId: Int) {
        val lista = _sharedItems.value.toMutableList()
        val index = lista.indexOfFirst { it.producto.id == productoId }
        if (index != -1) {
            val actual = lista[index]
            val paso = if (actual.producto.esPorPeso || actual.producto.tipoVenta == "PESO") 0.25 else 1.0
            val nuevaCant = actual.cantidad - paso
            if (nuevaCant > 0.01) {
                lista[index] = actual.copy(cantidad = nuevaCant)
            } else {
                lista.removeAt(index)
            }
            _sharedItems.value = lista.toList()
        }
    }

    fun eliminarPorId(productoId: Int) {
        val lista = _sharedItems.value.toMutableList()
        lista.removeAll { it.producto.id == productoId }
        _sharedItems.value = lista.toList()
    }

    fun vaciar() {
        _sharedItems.value = emptyList()
    }

    companion object {
        private val _sharedItems = MutableStateFlow<List<ItemCarrito>>(emptyList())
        val sharedItems: StateFlow<List<ItemCarrito>> get() = _sharedItems

        private var instance: CarritoViewModel? = null
        fun getInstance(): CarritoViewModel {
            if (instance == null) {
                instance = CarritoViewModel()
            }
            return instance!!
        }
    }
}
