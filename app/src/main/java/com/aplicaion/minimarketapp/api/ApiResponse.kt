package com.aplicaion.minimarketapp.api

/**
 * Envoltorio genérico para respuestas de API y operaciones del sistema.
 */
sealed class ApiResponse<out T> {
    data class Success<out T>(val data: T, val message: String = "Operación exitosa") : ApiResponse<T>()
    data class Error(val message: String, val code: Int = 400) : ApiResponse<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
}
