package com.aplicaion.minimarketapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.aplicaion.minimarketapp.db.entity.Venta
import com.aplicaion.minimarketapp.repository.ReporteRepository

class ReporteViewModel(private val reporteRepository: ReporteRepository) : ViewModel() {

    val ventas: LiveData<List<Venta>> = reporteRepository.getVentasFlow().asLiveData()

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
