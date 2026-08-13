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
    val cantidad: Int
) {
    val subtotalLinea: Double
        get() = producto.precioVenta * cantidad
}

class CarritoViewModel : ViewModel() {

    private val _items = _sharedItems
    val items: StateFlow<List<ItemCarrito>> = _items

    val totalItems: StateFlow<Int> = _items.map { list ->
        list.sumOf { it.cantidad }
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

    fun agregar(producto: Producto): Boolean {
        if (producto.stock <= 0) return false
        val lista = _sharedItems.value.toMutableList()
        val index = lista.indexOfFirst { it.producto.id == producto.id }
        if (index != -1) {
            val actual = lista[index]
            if (actual.cantidad >= producto.stock) {
                return false
            }
            lista[index] = actual.copy(cantidad = actual.cantidad + 1)
        } else {
            lista.add(ItemCarrito(producto, 1))
        }
        _sharedItems.value = lista.toList()
        return true
    }

    fun aumentarPorId(productoId: Int): Boolean {
        val lista = _sharedItems.value.toMutableList()
        val index = lista.indexOfFirst { it.producto.id == productoId }
        if (index != -1) {
            val actual = lista[index]
            if (actual.cantidad >= actual.producto.stock) {
                return false
            }
            lista[index] = actual.copy(cantidad = actual.cantidad + 1)
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
            if (actual.cantidad > 1) {
                lista[index] = actual.copy(cantidad = actual.cantidad - 1)
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
