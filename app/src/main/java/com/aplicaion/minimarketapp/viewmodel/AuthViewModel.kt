package com.aplicaion.minimarketapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aplicaion.minimarketapp.db.entity.Usuario
import com.aplicaion.minimarketapp.repository.AuthRepository
import com.aplicaion.minimarketapp.utils.Constants
import com.aplicaion.minimarketapp.utils.Resource
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableLiveData<Resource<Usuario>?>()
    val loginState: LiveData<Resource<Usuario>?> = _loginState

    private val _registroState = MutableLiveData<Resource<String>?>()
    val registroState: LiveData<Resource<String>?> = _registroState

    val usuarios: LiveData<List<Usuario>> = authRepository.getAllUsuarios().asLiveData()

    fun login(usuario: String, contrasena: String) {
        Log.d("LOGIN", "Intentando login con: $usuario / $contrasena")
        if (usuario.isBlank() || contrasena.isBlank()) {
            _loginState.value = Resource.Error("Por favor, ingrese usuario y contraseña")
            return
        }

        _loginState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val user = authRepository.login(usuario.trim(), contrasena.trim())
                Log.d("LOGIN", "Resultado: $user")
                if (user != null) {
                    _loginState.value = Resource.Success(user)
                } else {
                    _loginState.value = Resource.Error("Usuario o contraseña incorrectos")
                }
            } catch (e: Exception) {
                Log.e("LOGIN", "Error en login", e)
                _loginState.value = Resource.Error("Error al iniciar sesión: ${e.localizedMessage}")
            }
        }
    }

    fun registrarUsuario(
        nombreCompleto: String,
        usuario: String,
        contrasena: String,
        confirmarContrasena: String,
        correo: String,
        rol: String = Constants.ROL_VENDEDOR,
        estado: String = Constants.ESTADO_PENDIENTE
    ) {
        val validacion = com.aplicaion.minimarketapp.api.JsonDatabaseManager.validarUsuario(
            nombreCompleto = nombreCompleto,
            usuario = usuario,
            correo = correo,
            contrasena = contrasena,
            confirmarContrasena = confirmarContrasena
        )
        if (!validacion.isValid) {
            _registroState.value = Resource.Error(validacion.message)
            return
        }

        _registroState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                if (authRepository.existeUsuario(usuario.trim())) {
                    _registroState.value = Resource.Error("El nombre de usuario '${usuario.trim()}' ya está registrado")
                    return@launch
                }

                if (authRepository.existeCorreo(correo.trim())) {
                    _registroState.value = Resource.Error("El correo '${correo.trim()}' ya está registrado con otra cuenta")
                    return@launch
                }

                val nuevoUsuario = Usuario(
                    nombreCompleto = nombreCompleto.trim(),
                    usuario = usuario.trim(),
                    correo = correo.trim(),
                    contrasena = contrasena.trim(),
                    rol = rol,
                    estado = estado
                )

                authRepository.registrarUsuario(nuevoUsuario)
                val msg = if (estado == Constants.ESTADO_PENDIENTE) {
                    "Solicitud de acceso enviada. El Administrador debe aprobar tu registro."
                } else {
                    "Usuario registrado exitosamente"
                }
                _registroState.value = Resource.Success(msg)
            } catch (e: Exception) {
                _registroState.value = Resource.Error("Error al registrar usuario: ${e.localizedMessage}")
            }
        }
    }

    fun actualizarUsuario(usuario: Usuario) {
        _registroState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                authRepository.updateUsuario(usuario)
                _registroState.value = Resource.Success("Usuario actualizado exitosamente")
            } catch (e: Exception) {
                _registroState.value = Resource.Error("Error al actualizar usuario: ${e.localizedMessage}")
            }
        }
    }

    fun eliminarUsuario(usuario: Usuario) {
        _registroState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                authRepository.deleteUsuario(usuario)
                _registroState.value = Resource.Success("Usuario eliminado exitosamente")
            } catch (e: Exception) {
                _registroState.value = Resource.Error("Error al eliminar usuario: ${e.localizedMessage}")
            }
        }
    }

    fun resetStates() {
        _loginState.value = null
        _registroState.value = null
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
