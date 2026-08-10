package com.aplicaion.minimarketapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aplicaion.minimarketapp.db.entity.Categoria
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.repository.CategoriaRepository
import com.aplicaion.minimarketapp.repository.ProductoRepository
import com.aplicaion.minimarketapp.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class ProductoViewModel(
    private val productoRepository: ProductoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoriaId = MutableStateFlow<Int?>(null)

    val productos: LiveData<List<Producto>> = productoRepository.getAllProductos().asLiveData()

    val categorias: LiveData<List<Categoria>> = categoriaRepository.getAllCategorias().asLiveData()

    private val _guardarState = MutableLiveData<Resource<String>?>()
    val guardarState: LiveData<Resource<String>?> = _guardarState

    fun searchProductos(query: String): LiveData<List<Producto>> {
        return productoRepository.searchProductos(query).asLiveData()
    }

    fun getProductosByCategoria(categoriaId: Int): LiveData<List<Producto>> {
        return productoRepository.getProductosByCategoria(categoriaId).asLiveData()
    }

    fun calcularGanancia(precioCompra: Double, precioVenta: Double): Double {
        return Math.max(0.0, precioVenta - precioCompra)
    }

    fun guardarProducto(
        nombre: String,
        stockStr: String,
        categoriaId: Int,
        proveedorNombre: String,
        precioCompraStr: String,
        precioVentaStr: String,
        codigoBarras: String,
        descripcion: String,
        imagenPath: String?
    ) {
        if (nombre.isBlank()) {
            _guardarState.value = Resource.Error("Ingrese el nombre del producto")
            return
        }

        val stock = stockStr.toIntOrNull()
        if (stock == null || stock < 0) {
            _guardarState.value = Resource.Error("Ingrese un stock válido (no negativo)")
            return
        }

        if (categoriaId <= 0) {
            _guardarState.value = Resource.Error("Seleccione una categoría válida")
            return
        }

        val precioCompra = precioCompraStr.toDoubleOrNull()
        val precioVenta = precioVentaStr.toDoubleOrNull()

        if (precioCompra == null || precioCompra < 0) {
            _guardarState.value = Resource.Error("Ingrese un precio de compra válido")
            return
        }

        if (precioVenta == null || precioVenta < 0) {
            _guardarState.value = Resource.Error("Ingrese un precio de venta válido")
            return
        }

        if (codigoBarras.isBlank()) {
            _guardarState.value = Resource.Error("Ingrese o escanee un código de barras")
            return
        }

        _guardarState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val existe = productoRepository.getByCodigo(codigoBarras.trim())
                if (existe != null) {
                    _guardarState.value = Resource.Error("El código de barras '$codigoBarras' ya está registrado")
                    return@launch
                }

                val producto = Producto(
                    nombre = nombre.trim(),
                    categoriaId = categoriaId,
                    precioCompra = precioCompra,
                    precioVenta = precioVenta,
                    stock = stock,
                    descripcion = descripcion.trim(),
                    codigoBarras = codigoBarras.trim(),
                    imagenPath = imagenPath
                )

                productoRepository.insertProducto(producto)
                _guardarState.value = Resource.Success("Producto registrado correctamente")
            } catch (e: Exception) {
                _guardarState.value = Resource.Error("Error al guardar producto: ${e.localizedMessage}")
            }
        }
    }

    fun sumarStock(productoId: Int, cantidad: Int) {
        viewModelScope.launch {
            productoRepository.updateStock(productoId, cantidad)
        }
    }

    fun resetGuardarState() {
        _guardarState.value = null
    }

    class Factory(
        private val productoRepository: ProductoRepository,
        private val categoriaRepository: CategoriaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProductoViewModel::class.java)) {
                return ProductoViewModel(productoRepository, categoriaRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
