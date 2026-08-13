package com.aplicaion.minimarketapp.api

import android.content.Context

/**
 * Proveedor Singleton de la API del Minimarket.
 *
 * Facilita el desacoplamiento:
 * Cuando se desee migrar a un backend en la nube (ej. API REST con Node.js, Spring Boot, Firebase, etc.),
 * simplemente se cambia la instancia retornada aquí por una que implemente MinimarketApi con Retrofit/Ktor.
 */
object MinimarketApiProvider {

    @Volatile
    private var instance: MinimarketApi? = null

    fun getApi(context: Context): MinimarketApi {
        return instance ?: synchronized(this) {
            instance ?: MinimarketLocalJsonApiImpl(context.applicationContext).also {
                instance = it
            }
        }
    }

    /**
     * Permite inyectar una implementación remota o mock para pruebas / migración a la nube.
     */
    fun setCustomApi(customApi: MinimarketApi) {
        instance = customApi
    }
}
