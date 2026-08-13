package com.aplicaion.minimarketapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.aplicaion.minimarketapp.db.entity.Usuario

/**
 * Gestor de Sesión de Usuario en SharedPreferences.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("minimarket_user_session", Context.MODE_PRIVATE)

    fun saveUserSession(usuario: Usuario) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, usuario.id)
            putString(KEY_USER_NAME, usuario.nombreCompleto)
            putString(KEY_USERNAME, usuario.usuario)
            putString(KEY_USER_EMAIL, usuario.correo)
            putString(KEY_USER_ROLE, usuario.rol.uppercase())
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    val userId: Int get() = prefs.getInt(KEY_USER_ID, 0)
    val userName: String get() = prefs.getString(KEY_USER_NAME, "Usuario") ?: "Usuario"
    val username: String get() = prefs.getString(KEY_USERNAME, "") ?: ""
    val userEmail: String get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""
    val userRole: String get() = prefs.getString(KEY_USER_ROLE, Constants.ROL_VENDEDOR) ?: Constants.ROL_VENDEDOR
    val isLoggedIn: Boolean get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    val isAdmin: Boolean get() = userRole.equals(Constants.ROL_ADMIN, ignoreCase = true)
    val isVendedor: Boolean get() = userRole.equals(Constants.ROL_VENDEDOR, ignoreCase = true)

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun clearSession() {
        logout()
    }

    companion object {
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USERNAME = "key_username"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_ROLE = "key_user_role"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
