package com.aplicaion.minimarketapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.repository.AuthRepository
import com.aplicaion.minimarketapp.utils.Resource
import com.aplicaion.minimarketapp.viewmodel.AuthViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class registro_usuario : AppCompatActivity() {

    private lateinit var etNombreCompleto: TextInputEditText
    private lateinit var etUsuario: TextInputEditText
    private lateinit var etContrasena: TextInputEditText
    private lateinit var etCorreo: TextInputEditText
    private lateinit var btnAcceder: MaterialButton
    private lateinit var btnRegresar: ImageView

    private val authViewModel: AuthViewModel by viewModels {
        val db = AppDatabase.getInstance(this)
        val repo = AuthRepository(db.usuarioDao())
        AuthViewModel.Factory(repo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro_usuario)

        etNombreCompleto = findViewById(R.id.etNombreCompleto)
        etUsuario = findViewById(R.id.etUsuario)
        etContrasena = findViewById(R.id.etContrasena)
        etCorreo = findViewById(R.id.etCorreo)
        btnAcceder = findViewById(R.id.btnAcceder)
        btnRegresar = findViewById(R.id.btnRegresar)

        btnRegresar.setOnClickListener {
            finish()
        }

        btnAcceder.setOnClickListener {
            val nombre = etNombreCompleto.text?.toString().orEmpty()
            val user = etUsuario.text?.toString().orEmpty()
            val pass = etContrasena.text?.toString().orEmpty()
            val correo = etCorreo.text?.toString().orEmpty()

            authViewModel.registrarUsuario(
                nombreCompleto = nombre,
                usuario = user,
                contrasena = pass,
                confirmarContrasena = pass,
                correo = correo
            )
        }

        authViewModel.registroState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    btnAcceder.isEnabled = false
                }
                is Resource.Success -> {
                    btnAcceder.isEnabled = true
                    Toast.makeText(this, resource.data ?: "Registro exitoso", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resource.Error -> {
                    btnAcceder.isEnabled = true
                    Toast.makeText(this, resource.message ?: "Error al registrar", Toast.LENGTH_LONG).show()
                }
                null -> {
                    btnAcceder.isEnabled = true
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
