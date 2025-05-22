
package com.example.proyectomov

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.Spinner
import java.util.Calendar
import android.app.DatePickerDialog
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

data class ReporteIngreso(
    val usuario: String,
    val mes: Int,
    val anio: Int,
    val tipoMovimiento: String,
    val totalGeneral: Double,
    val totalPorTipo: List<TotalPorTipo>
)

data class TotalPorTipo(
    val tipo: String,
    val total: Double
)

data class ReporteGasto(
    val nombre: String,
    val fecha: String,
    val tipo: String,
    val monto: Double,
    val archivo: String
)


class Report : AppCompatActivity() {
    private var usuarioID: String = ""
    private var mesSeleccionado: Int = -1
    private var anioSeleccionado: Int = -1
    private var tipoSeleccionado: String = "ingreso"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte)

        usuarioID = UsuarioGlobal.id.toString()



        val spinnerTipo: Spinner = findViewById(R.id.spinner2)
        val opcionesTipo = listOf("Ingreso", "Gasto")

        val adapterTipo = ArrayAdapter(this, android.R.layout.simple_spinner_item, opcionesTipo)
        adapterTipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipo.adapter = adapterTipo
        spinnerTipo.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                tipoSeleccionado = parent?.getItemAtPosition(position).toString().lowercase()

                // Solo llamar si ya se eligió una fecha válida
                if (mesSeleccionado > 0 && anioSeleccionado > 0) {
                    obtenerReporteIngresos(usuarioID, mesSeleccionado, anioSeleccionado, tipoSeleccionado)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // No hacer nada
            }
        }

        val textViewFecha: TextView = findViewById(R.id.textViewFecha)

        textViewFecha.setOnClickListener {
            mostrarSelectorFecha(textViewFecha)
        }

        val seleccionTipo = spinnerTipo.selectedItem.toString()



    }

    private fun mostrarSelectorFecha(textView: TextView) {
        val calendario = Calendar.getInstance()
        val anio = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)

        val datePicker = DatePickerDialog(this, { _, anioSel, mesSel, _ ->
            val meses = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")

            val mesTexto = meses[mesSel]
            textView.text = "$mesTexto $anioSel"

            // Guardar valores
            mesSeleccionado = mesSel + 1 // Importante: +1 porque Calendar.MONTH va de 0 a 11
            anioSeleccionado = anioSel

            // Obtener tipo seleccionado del Spinner
            val tipo = findViewById<Spinner>(R.id.spinner2).selectedItem.toString().lowercase()

            tipoSeleccionado = tipo



            // Llamar a la función para obtener el reporte
            obtenerReporteIngresos(usuarioID, mesSeleccionado, anioSeleccionado, tipoSeleccionado)
        }, anio, mes, 1)

        datePicker.show()
    }


    private fun obtenerReporteIngresos(usuarioID: String, mes: Int, anio: Int, tipoMovimiento: String) {
        val baseUrl = getString(R.string.base_url)


        val url = "$baseUrl/reportes/reportePorMes?usuarioID=$usuarioID&mes=$mes&anio=$anio&tipoMovimiento=$tipoMovimiento"

        Log.d("DEBUG", "UsuarioID: $url")

        val requestQueue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val usuario = response.getString("usuario")
                    val mesResp = response.getInt("mes")
                    val anioResp = response.getInt("anio")
                    val tipoMovimiento = response.getString("tipoMovimiento")
                    val totalGeneral = response.getDouble("totalGeneral")

                    val totalPorTipoArray = response.getJSONArray("totalPorTipo")
                    val listaTotales = mutableListOf<TotalPorTipo>()

                    for (i in 0 until totalPorTipoArray.length()) {
                        val item = totalPorTipoArray.getJSONObject(i)
                        val tipo = item.getString("tipo")
                        val total = item.getDouble("total")

                        listaTotales.add(TotalPorTipo(tipo, total))
                    }

                    val reporte = ReporteIngreso(
                        usuario = usuario,
                        mes = mesResp,
                        anio = anioResp,
                        tipoMovimiento = tipoMovimiento,
                        totalGeneral = totalGeneral,
                        totalPorTipo = listaTotales
                    )

                    mostrarReporte(reporte)

                } catch (e: Exception) {
                    Log.e("ParseError", "Error al parsear: ${e.message}")
                    Toast.makeText(this, "Error al procesar datos", Toast.LENGTH_LONG).show()
                }
            },
            { error ->
                Log.e("VolleyError", "Error de red: ${error.message}", error)
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )

        requestQueue.add(request)
    }

    private fun mostrarReporte(reporte: ReporteIngreso) {
        // Mostrar el total general en el TextView
        val totalTextView = findViewById<TextView>(R.id.amount15)
        totalTextView.text = "Total: $${reporte.totalGeneral}"

        // Mostrar cada tipo con su total
        val layoutTotales = findViewById<LinearLayout>(R.id.layoutTotalesPorTipo)
        layoutTotales.removeAllViews() // Limpiar antes de añadir nuevos

        for (tipo in reporte.totalPorTipo) {
            val tipoTextView = TextView(this).apply {
                text = "${tipo.tipo}: $${tipo.total}"
                textSize = 18f
                setTextColor(ContextCompat.getColor(context, R.color.white))
                typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
                gravity = android.view.Gravity.CENTER
                setPadding(0, 16, 0, 16) // espacio vertical
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 8, 16, 8) // margen alrededor del TextView
                }

            }
            layoutTotales.addView(tipoTextView)
        }
    }


}