package com.aplicaion.minimarketapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aplicaion.minimarketapp.db.entity.Proveedor
import com.aplicaion.minimarketapp.repository.ProveedorRepository
import com.aplicaion.minimarketapp.utils.Resource
import kotlinx.coroutines.launch

class ProveedorViewModel(private val proveedorRepository: ProveedorRepository) : ViewModel() {

    val proveedores: LiveData<List<Proveedor>> = proveedorRepository.getAllProveedores().asLiveData()

    private val _guardarState = MutableLiveData<Resource<String>?>()
    val guardarState: LiveData<Resource<String>?> = _guardarState

    fun registrarProveedor(
        nombre: String,
        ruc: String,
        celular: String,
        direccion: String,
        correo: String
    ) {
        if (nombre.isBlank() || ruc.isBlank()) {
            _guardarState.value = Resource.Error("Nombre y RUC son requeridos")
            return
        }

        _guardarState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                if (proveedorRepository.getByRuc(ruc.trim()) != null) {
                    _guardarState.value = Resource.Error("El RUC ya se encuentra registrado")
                    return@launch
                }

                val proveedor = Proveedor(
                    nombre = nombre.trim(),
                    ruc = ruc.trim(),
                    celular = celular.trim(),
                    direccion = direccion.trim(),
                    correo = correo.trim()
                )

                proveedorRepository.insertProveedor(proveedor)
                _guardarState.value = Resource.Success("Proveedor registrado exitosamente")
            } catch (e: Exception) {
                _guardarState.value = Resource.Error("Error al guardar proveedor: ${e.localizedMessage}")
            }
        }
    }

    fun actualizarProveedor(proveedor: Proveedor) {
        if (proveedor.nombre.isBlank() || proveedor.ruc.isBlank()) {
            _guardarState.value = Resource.Error("Nombre y RUC son requeridos")
            return
        }
        _guardarState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                proveedorRepository.updateProveedor(proveedor)
                _guardarState.value = Resource.Success("Proveedor actualizado exitosamente")
            } catch (e: Exception) {
                _guardarState.value = Resource.Error("Error al actualizar proveedor: ${e.localizedMessage}")
            }
        }
    }

    fun eliminarProveedor(proveedor: Proveedor) {
        _guardarState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                proveedorRepository.deleteProveedor(proveedor)
                _guardarState.value = Resource.Success("Proveedor eliminado exitosamente")
            } catch (e: Exception) {
                _guardarState.value = Resource.Error("Error al eliminar proveedor: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _guardarState.value = null
    }

    class Factory(private val proveedorRepository: ProveedorRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProveedorViewModel::class.java)) {
                return ProveedorViewModel(proveedorRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
