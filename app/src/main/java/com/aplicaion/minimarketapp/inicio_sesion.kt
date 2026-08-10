package com.aplicaion.minimarketapp

import android.content.Intent
import android.os.Bundle
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
import android.widget.TextView

class inicio_sesion : AppCompatActivity() {

    private lateinit var txtUsuario: TextInputEditText
    private lateinit var txtContrasena: TextInputEditText
    private lateinit var btnIngresar: MaterialButton
    private lateinit var btnRegistroUsuario: TextView

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

        btnIngresar.setOnClickListener {
            val user = txtUsuario.text?.toString().orEmpty()
            val pass = txtContrasena.text?.toString().orEmpty()
            authViewModel.login(user, pass)
        }

        btnRegistroUsuario.setOnClickListener {
            val intent = Intent(this, registro_usuario::class.java)
            startActivity(intent)
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
                    Toast.makeText(this, "Bienvenido, ${usuario?.nombreCompleto ?: ""}", Toast.LENGTH_SHORT).show()
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
}
