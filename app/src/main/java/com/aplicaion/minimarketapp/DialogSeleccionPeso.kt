package com.aplicaion.minimarketapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.aplicaion.minimarketapp.db.entity.Producto
import com.aplicaion.minimarketapp.utils.formatSoles
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class DialogSeleccionPeso(
    private val producto: Producto,
    private val onPesoConfirmado: (pesoKg: Double, etiqueta: String) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var tvNombreProductoPeso: TextView
    private lateinit var tvPrecioPorKilo: TextView
    private lateinit var btnUnCuarto: MaterialButton
    private lateinit var btnMedioKilo: MaterialButton
    private lateinit var btnTresCuartos: MaterialButton
    private lateinit var btnUnKilo: MaterialButton
    private lateinit var etPesoPersonalizado: TextInputEditText
    private lateinit var tvTotalCalculado: TextView
    private lateinit var btnCancelarPeso: MaterialButton
    private lateinit var btnAgregarAlCarritoPeso: MaterialButton

    private var pesoSeleccionado: Double = 0.25
    private var etiquetaSeleccionada: String = "1/4 kg"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_seleccion_peso, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvNombreProductoPeso = view.findViewById(R.id.tvNombreProductoPeso)
        tvPrecioPorKilo = view.findViewById(R.id.tvPrecioPorKilo)
        btnUnCuarto = view.findViewById(R.id.btnUnCuarto)
        btnMedioKilo = view.findViewById(R.id.btnMedioKilo)
        btnTresCuartos = view.findViewById(R.id.btnTresCuartos)
        btnUnKilo = view.findViewById(R.id.btnUnKilo)
        etPesoPersonalizado = view.findViewById(R.id.etPesoPersonalizado)
        tvTotalCalculado = view.findViewById(R.id.tvTotalCalculado)
        btnCancelarPeso = view.findViewById(R.id.btnCancelarPeso)
        btnAgregarAlCarritoPeso = view.findViewById(R.id.btnAgregarAlCarritoPeso)

        tvNombreProductoPeso.text = producto.nombre
        tvPrecioPorKilo.text = "Precio por Kilo: ${producto.precioVenta.formatSoles()}"

        fun seleccionarBoton(btnActivo: MaterialButton, peso: Double, label: String) {
            pesoSeleccionado = peso
            etiquetaSeleccionada = label
            val botones = listOf(btnUnCuarto, btnMedioKilo, btnTresCuartos, btnUnKilo)
            botones.forEach { b ->
                if (b == btnActivo) {
                    b.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.morado_scanner))
                    b.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                } else {
                    b.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.fondo_card))
                    b.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_principal))
                }
            }
            actualizarTotal()
        }

        btnUnCuarto.setOnClickListener {
            etPesoPersonalizado.setText("")
            seleccionarBoton(btnUnCuarto, 0.25, "1/4 kg")
        }

        btnMedioKilo.setOnClickListener {
            etPesoPersonalizado.setText("")
            seleccionarBoton(btnMedioKilo, 0.50, "1/2 kg")
        }

        btnTresCuartos.setOnClickListener {
            etPesoPersonalizado.setText("")
            seleccionarBoton(btnTresCuartos, 0.75, "3/4 kg")
        }

        btnUnKilo.setOnClickListener {
            etPesoPersonalizado.setText("")
            seleccionarBoton(btnUnKilo, 1.00, "1 kg")
        }

        etPesoPersonalizado.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val valor = s?.toString()?.toDoubleOrNull()
                if (valor != null && valor > 0) {
                    pesoSeleccionado = valor
                    etiquetaSeleccionada = "$valor kg"
                    val botones = listOf(btnUnCuarto, btnMedioKilo, btnTresCuartos, btnUnKilo)
                    botones.forEach { b ->
                        b.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.fondo_card))
                        b.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_principal))
                    }
                    actualizarTotal()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnCancelarPeso.setOnClickListener {
            dismiss()
        }

        btnAgregarAlCarritoPeso.setOnClickListener {
            if (pesoSeleccionado <= 0) {
                Toast.makeText(requireContext(), "Ingresa un peso válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onPesoConfirmado(pesoSeleccionado, etiquetaSeleccionada)
            dismiss()
        }

        // Seleccionar 1/4 kg por defecto
        seleccionarBoton(btnUnCuarto, 0.25, "1/4 kg")
    }

    private fun actualizarTotal() {
        val total = pesoSeleccionado * producto.precioVenta
        tvTotalCalculado.text = total.formatSoles()
    }
}
