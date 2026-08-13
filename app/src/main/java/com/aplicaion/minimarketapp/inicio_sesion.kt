package com.aplicaion.minimarketapp

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.aplicaion.minimarketapp.api.ApiResponse
import com.aplicaion.minimarketapp.api.MinimarketApiProvider
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.repository.AuthRepository
import com.aplicaion.minimarketapp.utils.Resource
import com.aplicaion.minimarketapp.utils.SessionManager
import com.aplicaion.minimarketapp.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class inicio_sesion : AppCompatActivity() {

    private lateinit var txtUsuario: TextInputEditText
    private lateinit var txtContrasena: TextInputEditText
    private lateinit var btnIngresar: MaterialButton
    private lateinit var btnRegistroUsuario: TextView
    private lateinit var btnRecuperarCorreo: TextView

    private val authViewModel: AuthViewModel by viewModels {
        val db = AppDatabase.getInstance(this)
        val repo = AuthRepository(db.usuarioDao())
        AuthViewModel.Factory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inicio_sesion)

        txtUsuario = findViewById(R.id.txtUsuario)
        txtContrasena = findViewById(R.id.txtContraseña)
        btnIngresar = findViewById(R.id.btnIngresar)
        btnRegistroUsuario = findViewById(R.id.btnRegistroUsuario)
        btnRecuperarCorreo = findViewById(R.id.btnRecuperarCorreo)

        btnIngresar.setOnClickListener {
            val user = txtUsuario.text?.toString().orEmpty()
            val pass = txtContrasena.text?.toString().orEmpty()
            authViewModel.login(user, pass)
        }

        btnRegistroUsuario.setOnClickListener {
            val intent = Intent(this, registro_usuario::class.java)
            startActivity(intent)
        }

        btnRecuperarCorreo.setOnClickListener {
            mostrarDialogoRecuperarContrasena()
        }

        observeViewModel()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun observeViewModel() {
        authViewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    btnIngresar.isEnabled = false
                }
                is Resource.Success -> {
                    btnIngresar.isEnabled = true
                    val usuario = resource.data
                    if (usuario != null) {
                        SessionManager.getInstance(this).saveUserSession(usuario)
                        Toast.makeText(this, "Bienvenido, ${usuario.nombreCompleto} (${usuario.rol})", Toast.LENGTH_SHORT).show()
                    }
                    val intent = Intent(this, punto_venta::class.java).apply {
                        putExtra("USER_ROL", usuario?.rol ?: "VENDEDOR")
                        putExtra("USER_NAME", usuario?.nombreCompleto ?: "")
                    }
                    startActivity(intent)
                    finish()
                }
                is Resource.Error -> {
                    btnIngresar.isEnabled = true
                    Toast.makeText(this, resource.message ?: "Error al ingresar", Toast.LENGTH_LONG).show()
                }
                null -> {
                    btnIngresar.isEnabled = true
                }
            }
        }
    }

    private fun mostrarDialogoRecuperarContrasena() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 10)
        }

        val etUsuario = EditText(this).apply {
            hint = "Usuario o Nombre de Cuenta"
        }
        val etCorreo = EditText(this).apply {
            hint = "Correo Electrónico Registrado"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val etNuevaPass = EditText(this).apply {
            hint = "Nueva Contraseña (mín. 6 caracteres)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(etUsuario)
        layout.addView(etCorreo)
        layout.addView(etNuevaPass)

        AlertDialog.Builder(this)
            .setTitle("Recuperación de Contraseña")
            .setMessage("Ingresa los datos asociados a tu cuenta para restablecer tu contraseña:")
            .setView(layout)
            .setPositiveButton("Restablecer") { _, _ ->
                val user = etUsuario.text.toString().trim()
                val email = etCorreo.text.toString().trim()
                val newPass = etNuevaPass.text.toString().trim()

                if (email.isEmpty() || newPass.length < 6) {
                    Toast.makeText(this, "Completa el correo y una contraseña de al menos 6 caracteres", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val api = MinimarketApiProvider.getApi(this@inicio_sesion)
                    val res = api.recuperarContrasena(user, email, newPass)
                    when (res) {
                        is ApiResponse.Success -> {
                            Toast.makeText(this@inicio_sesion, res.message ?: "Contraseña actualizada exitosamente", Toast.LENGTH_LONG).show()
                        }
                        is ApiResponse.Error -> {
                            Toast.makeText(this@inicio_sesion, res.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
