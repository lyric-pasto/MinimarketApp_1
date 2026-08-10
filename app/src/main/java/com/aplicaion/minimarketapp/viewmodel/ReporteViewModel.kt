package com.aplicaion.minimarketapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import com.aplicaion.minimarketapp.db.entity.Venta
import com.aplicaion.minimarketapp.repository.ReporteRepository

class ReporteViewModel(private val reporteRepository: ReporteRepository) : ViewModel() {

    private val _rangoFechas = MutableLiveData<Pair<Long, Long>?>(null)
    private val _metodoPago = MutableLiveData<String?>(null)
    private val _searchQuery = MutableLiveData<String>("")

    private val rawVentas: LiveData<List<Venta>> = _rangoFechas.switchMap { rango ->
        if (rango == null) {
            reporteRepository.getVentasFlow().asLiveData()
        } else {
            reporteRepository.getVentasByFecha(rango.first, rango.second).asLiveData()
        }
    }

    val ventas = MediatorLiveData<List<Venta>>().apply {
        fun aplicarFiltros() {
            val list = rawVentas.value.orEmpty()
            val metodo = _metodoPago.value
            val query = _searchQuery.value?.trim()?.lowercase().orEmpty()

            value = list.filter { venta ->
                val cumpleMetodo = if (metodo.isNullOrEmpty() || metodo.equals("Todos", ignoreCase = true)) {
                    true
                } else {
                    venta.metodoPago.equals(metodo, ignoreCase = true)
                }

                val cumpleQuery = if (query.isEmpty()) {
                    true
                } else {
                    venta.codigoVenta.lowercase().contains(query)
                }

                cumpleMetodo && cumpleQuery
            }
        }

        addSource(rawVentas) { aplicarFiltros() }
        addSource(_metodoPago) { aplicarFiltros() }
        addSource(_searchQuery) { aplicarFiltros() }
    }

    fun filtrarPorFecha(inicio: Long, fin: Long) {
        _rangoFechas.value = Pair(inicio, fin)
    }

    fun filtrarPorMetodoPago(metodo: String?) {
        _metodoPago.value = metodo
    }

    fun buscarPorQuery(query: String) {
        _searchQuery.value = query
    }

    fun limpiarFiltros() {
        _rangoFechas.value = null
        _metodoPago.value = null
        _searchQuery.value = ""
    }

    class Factory(private val reporteRepository: ReporteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReporteViewModel::class.java)) {
                return ReporteViewModel(reporteRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
