package com.aplicaion.minimarketapp

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.aplicaion.minimarketapp.db.AppDatabase
import com.aplicaion.minimarketapp.repository.VentaRepository
import com.aplicaion.minimarketapp.utils.formatSoles
import com.aplicaion.minimarketapp.viewmodel.CarritoViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class DialogPagoFragment : BottomSheetDialogFragment() {

    private lateinit var spinnerTipoCliente: Spinner
    private lateinit var tvTotalPagar: TextView
    private lateinit var btnEfectivo: MaterialButton
    private lateinit var btnYape: MaterialButton

    private val carritoViewModel: CarritoViewModel = CarritoViewModel.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_pago, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerTipoCliente = view.findViewById(R.id.spinnerTipoCliente)
        tvTotalPagar = view.findViewById(R.id.tvTotalPagar)
        btnEfectivo = view.findViewById(R.id.btnEfectivo)
        btnYape = view.findViewById(R.id.btnYape)

        val opcionesCliente = listOf("Cliente General", "Cliente Frecuente")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, opcionesCliente)
        spinnerTipoCliente.adapter = adapter

        val items = carritoViewModel.items.value
        val total = items.sumOf { it.producto.precioVenta * it.cantidad }

        tvTotalPagar.text = total.formatSoles()

        btnEfectivo.setOnClickListener {
            procesarPagoEfectivo(total)
        }

        btnYape.setOnClickListener {
            procesarPagoYape(total)
        }
    }

    private fun procesarPagoEfectivo(total: Double) {
        val pagoEfectivo = PagoEfectivoFragment()
        pagoEfectivo.show(parentFragmentManager, "PagoEfectivoFragment")
    }

    private fun procesarPagoYape(total: Double) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_yape, null)
        val tvMontoYape = dialogView.findViewById<TextView>(R.id.tvMontoYape)
        val btnCerrarYape = dialogView.findViewById<MaterialButton>(R.id.btnCerrarYape)

        tvMontoYape.text = "Monto a pagar: ${total.formatSoles()}"

        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        btnCerrarYape.setOnClickListener {
            alertDialog.dismiss()
            ejecutarRegistroVenta("YAPE")
        }

        alertDialog.show()
    }

    private fun ejecutarRegistroVenta(metodoPago: String) {
        val ctx = context ?: return
        val items = carritoViewModel.items.value
        if (items.isEmpty()) {
            Toast.makeText(ctx, "El carrito está vacío", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val db = AppDatabase.getInstance(ctx)
            val repo = VentaRepository(db.ventaDao(), db.detalleVentaDao(), db.productoDao())
            val res = repo.registrarVenta(items, metodoPago)
            res.fold(
                onSuccess = {
                    carritoViewModel.vaciar()
                    Toast.makeText(ctx, "¡Venta registrada!", Toast.LENGTH_SHORT).show()
                    dismiss()
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
