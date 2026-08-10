package com.aplicaion.minimarketapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.db.entity.Venta
import com.aplicaion.minimarketapp.repository.ItemCarrito
import com.aplicaion.minimarketapp.repository.VentaRepository
import com.aplicaion.minimarketapp.utils.Resource
import kotlinx.coroutines.launch

class VentaViewModel(private val ventaRepository: VentaRepository) : ViewModel() {

    val ventas: LiveData<List<Venta>> = ventaRepository.getAllVentas().asLiveData()

    private val _carrito = MutableLiveData<List<ItemCarrito>>(emptyList())
    val carrito: LiveData<List<ItemCarrito>> = _carrito

    private val _subtotal = MutableLiveData(0.0)
    val subtotal: LiveData<Double> = _subtotal

    private val _igv = MutableLiveData(0.0)
    val igv: LiveData<Double> = _igv

    private val _total = MutableLiveData(0.0)
    val total: LiveData<Double> = _total

    private val _ventaResult = MutableLiveData<Resource<String>?>()
    val ventaResult: LiveData<Resource<String>?> = _ventaResult

    fun agregarAlCarrito(producto: Producto) {
        val list = _carrito.value.orEmpty().toMutableList()
        val index = list.indexOfFirst { it.producto.id == producto.id }

        if (index != -1) {
            val actual = list[index]
            if (actual.cantidad + 1 > producto.stock) {
                _ventaResult.value = Resource.Error("No hay suficiente stock para añadir más unidades")
                return
            }
            list[index] = actual.copy(cantidad = actual.cantidad + 1)
        } else {
            if (producto.stock < 1) {
                _ventaResult.value = Resource.Error("Producto sin stock disponible")
                return
            }
            list.add(ItemCarrito(producto, 1))
        }

        _carrito.value = list
        recalcularTotales()
    }

    fun modificarCantidad(productoId: Int, delta: Int) {
        val list = _carrito.value.orEmpty().toMutableList()
        val index = list.indexOfFirst { it.producto.id == productoId }

        if (index != -1) {
            val item = list[index]
            val nuevaCantidad = item.cantidad + delta
            if (nuevaCantidad <= 0) {
                list.removeAt(index)
            } else {
                if (nuevaCantidad > item.producto.stock) {
                    _ventaResult.value = Resource.Error("Stock máximo superado (${item.producto.stock})")
                    return
                }
                list[index] = item.copy(cantidad = nuevaCantidad)
            }
            _carrito.value = list
            recalcularTotales()
        }
    }

    fun eliminarDelCarrito(productoId: Int) {
        val list = _carrito.value.orEmpty().toMutableList()
        list.removeAll { it.producto.id == productoId }
        _carrito.value = list
        recalcularTotales()
    }

    fun limpiarCarrito() {
        _carrito.value = emptyList()
        recalcularTotales()
    }

    private fun recalcularTotales() {
        val items = _carrito.value.orEmpty()
        val sub = items.sumOf { it.subtotalLinea }
        val igvVal = Math.round(sub * 0.18 * 100.0) / 100.0
        val totVal = Math.round((sub + igvVal) * 100.0) / 100.0

        _subtotal.value = sub
        _igv.value = igvVal
        _total.value = totVal
    }

    fun procesarVenta(metodoPago: String) {
        val items = _carrito.value.orEmpty()
        if (items.isEmpty()) {
            _ventaResult.value = Resource.Error("El carrito está vacío")
            return
        }

        _ventaResult.value = Resource.Loading()
        viewModelScope.launch {
            val res = ventaRepository.registrarVenta(items, metodoPago)
            res.fold(
                onSuccess = {
                    limpiarCarrito()
                    _ventaResult.value = Resource.Success("Venta registrada exitosamente")
                },
                onFailure = { err ->
                    _ventaResult.value = Resource.Error(err.localizedMessage ?: "Error al procesar la venta")
                }
            )
        }
    }

    fun resetVentaResult() {
        _ventaResult.value = null
    }

    class Factory(private val ventaRepository: VentaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VentaViewModel::class.java)) {
                return VentaViewModel(ventaRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
