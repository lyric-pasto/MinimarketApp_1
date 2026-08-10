package com.aplicaion.minimarketapp

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.repository.VentaRepository
import com.aplicaion.minimarketapp.utils.formatSoles
import com.aplicaion.minimarketapp.viewmodel.CarritoViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class PagoEfectivoFragment : BottomSheetDialogFragment() {

    private lateinit var tvTotalCobrar: TextView
    private lateinit var etMontoRecibido: TextInputEditText
    private lateinit var cardVuelto: MaterialCardView
    private lateinit var tvRecibido: TextView
    private lateinit var tvVuelto: TextView
    private lateinit var btnConfirmarEfectivo: MaterialButton

    private val carritoViewModel = CarritoViewModel.getInstance()
    private var totalPagar: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_pago_efectivo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTotalCobrar = view.findViewById(R.id.tvTotalCobrar)
        etMontoRecibido = view.findViewById(R.id.etMontoRecibido)
        cardVuelto = view.findViewById(R.id.cardVuelto)
        tvRecibido = view.findViewById(R.id.tvRecibido)
        tvVuelto = view.findViewById(R.id.tvVuelto)
        btnConfirmarEfectivo = view.findViewById(R.id.btnConfirmarEfectivo)

        val items = carritoViewModel.items.value
        totalPagar = items.sumOf { it.producto.precioVenta * it.cantidad }

        tvTotalCobrar.text = "Total: ${totalPagar.formatSoles()}"

        etMontoRecibido.addTextChangedListener {
            val recibido = it.toString().toDoubleOrNull() ?: 0.0
            if (recibido >= totalPagar && totalPagar > 0) {
                val vuelto = recibido - totalPagar
                tvRecibido.text = recibido.formatSoles()
                tvVuelto.text = vuelto.formatSoles()
                cardVuelto.visibility = View.VISIBLE
                btnConfirmarEfectivo.isEnabled = true
                tvVuelto.setTextColor(
                    if (vuelto > 0) androidx.core.content.ContextCompat.getColor(requireContext(), R.color.verde_ganancia)
                    else androidx.core.content.ContextCompat.getColor(requireContext(), R.color.texto_hint)
                )
            } else {
                cardVuelto.visibility = View.GONE
                btnConfirmarEfectivo.isEnabled = false
            }
        }

        btnConfirmarEfectivo.setOnClickListener {
            ejecutarVentaEfectivo()
        }
    }

    private fun ejecutarVentaEfectivo() {
        val ctx = context ?: return
        val items = carritoViewModel.items.value
        if (items.isEmpty()) {
            Toast.makeText(ctx, "El carrito está vacío", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(ctx)
            val repo = VentaRepository(db.ventaDao(), db.detalleVentaDao(), db.productoDao())
            val res = repo.registrarVenta(items, "EFECTIVO")
            res.fold(
                onSuccess = {
                    carritoViewModel.vaciar()
                    Toast.makeText(ctx, "✓ Venta registrada", Toast.LENGTH_SHORT).show()
                    dismiss()
                    parentFragmentManager.fragments.forEach { fragment ->
                        if (fragment is DialogPagoFragment) {
                            fragment.dismiss()
                        }
                    }
                    activity?.let { act ->
                        if (act !is punto_venta) {
                            act.finish()
                        }
                    }
                },
                onFailure = { err ->
                    Toast.makeText(ctx, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
