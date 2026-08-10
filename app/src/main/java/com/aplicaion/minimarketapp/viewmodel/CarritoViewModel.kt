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
    var cantidad: Int
) {
    val subtotalLinea: Double
        get() = producto.precioVenta * cantidad
}

class CarritoViewModel : ViewModel() {

    private val _items = _sharedItems
    val items: StateFlow<List<ItemCarrito>> = _items

    val totalItems: StateFlow<Int> = _items.map { list ->
        list.sumOf { it.cantidad }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val subtotal: StateFlow<Double> = _items.map { list ->
        list.sumOf { it.producto.precioVenta * it.cantidad }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    fun agregar(producto: Producto) {
        val lista = _sharedItems.value.toMutableList()
        val existente = lista.find { it.producto.id == producto.id }
        if (existente != null) {
            existente.cantidad++
        } else {
            lista.add(ItemCarrito(producto, 1))
        }
        _sharedItems.value = lista
    }

    fun reducir(producto: Producto) {
        val lista = _sharedItems.value.toMutableList()
        val existente = lista.find { it.producto.id == producto.id }
        if (existente != null) {
            if (existente.cantidad > 1) {
                existente.cantidad--
            } else {
                lista.remove(existente)
            }
        }
        _sharedItems.value = lista
    }

    fun reducirPorId(productoId: Int) {
        val lista = _sharedItems.value.toMutableList()
        val existente = lista.find { it.producto.id == productoId }
        if (existente != null) {
            if (existente.cantidad > 1) {
                existente.cantidad--
            } else {
                lista.remove(existente)
            }
        }
        _sharedItems.value = lista
    }

    fun eliminarPorId(productoId: Int) {
        val lista = _sharedItems.value.toMutableList()
        lista.removeAll { it.producto.id == productoId }
        _sharedItems.value = lista
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
