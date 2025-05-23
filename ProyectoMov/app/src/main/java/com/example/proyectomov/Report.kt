package com.example.proyectomov

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import android.app.DatePickerDialog
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup // Import RadioGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

// Data classes permanecen igual
data class ReporteData( // Renombrada desde ReporteIngreso para generalizar
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

// ReporteGasto no se usa actualmente en esta lógica, pero se mantiene por si acaso
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
    private var tipoSeleccionado: String = "gastos" // Default to "gastos" as it's checked by default in XML

    private lateinit var radioGroupTipoReporte: RadioGroup // Declarar RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reporte)

        usuarioID = UsuarioGlobal.id.toString()

        val backArrow: ImageView = findViewById(R.id.imageView4)
        backArrow.setOnClickListener {
            finish()
        }

        radioGroupTipoReporte = findViewById(R.id.radioGroupTipoTransaccion)
        radioGroupTipoReporte.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radioButtonGasto -> tipoSeleccionado = "gastos"
                R.id.radioButtonIngreso -> tipoSeleccionado = "ingresos"
            }
            if (mesSeleccionado > 0 && anioSeleccionado > 0) {
                obtenerReporte(usuarioID, mesSeleccionado, anioSeleccionado, tipoSeleccionado)
            }
        }

        if (radioGroupTipoReporte.checkedRadioButtonId == R.id.radioButtonGasto) {
            tipoSeleccionado = "gasto"
        } else if (radioGroupTipoReporte.checkedRadioButtonId == R.id.radioButtonIngreso) {
            tipoSeleccionado = "ingreso"
        }


        val textViewFecha: TextView = findViewById(R.id.textViewFecha)
        textViewFecha.setOnClickListener {
            mostrarSelectorFecha(textViewFecha)
        }

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

            mesSeleccionado = mesSel + 1
            anioSeleccionado = anioSel

            obtenerReporte(usuarioID, mesSeleccionado, anioSeleccionado, tipoSeleccionado)
        }, anio, mes, 1)

        datePicker.show()
    }


    private fun obtenerReporte(usuarioID: String, mes: Int, anio: Int, tipoMovimiento: String) {
        val baseUrl = getString(R.string.base_url)
        val url = "$baseUrl/reportes/reportePorMes?usuarioID=$usuarioID&mes=$mes&anio=$anio&tipoMovimiento=$tipoMovimiento"

        Log.d("DEBUG", "Reporte URL: $url")

        val requestQueue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val usuario = response.getString("usuario")
                    val mesResp = response.getInt("mes")
                    val anioResp = response.getInt("anio")
                    val tipoMovResp = response.getString("tipoMovimiento")
                    val totalGeneral = response.getDouble("totalGeneral")

                    val totalPorTipoArray = response.getJSONArray("totalPorTipo")
                    val listaTotales = mutableListOf<TotalPorTipo>()

                    for (i in 0 until totalPorTipoArray.length()) {
                        val item = totalPorTipoArray.getJSONObject(i)
                        val tipo = item.getString("tipo")
                        val total = item.getDouble("total")
                        listaTotales.add(TotalPorTipo(tipo, total))
                    }

                    val reporte = ReporteData(
                        usuario = usuario,
                        mes = mesResp,
                        anio = anioResp,
                        tipoMovimiento = tipoMovResp,
                        totalGeneral = totalGeneral,
                        totalPorTipo = listaTotales
                    )

                    mostrarReporte(reporte)

                } catch (e: Exception) {
                    Log.e("ParseError", "Error al parsear: ${e.message}", e)
                    Toast.makeText(this, "Error al procesar datos del reporte", Toast.LENGTH_LONG).show()
                    findViewById<TextView>(R.id.amount15).text = "Total: $0.00"
                    findViewById<LinearLayout>(R.id.layoutTotalesPorTipo).removeAllViews()
                }
            },
            { error ->
                Log.e("VolleyError", "Error de red al obtener reporte: ${error.message}", error)
                Toast.makeText(this, "Error de red: ${error.message}", Toast.LENGTH_LONG).show()

                findViewById<TextView>(R.id.amount15).text = "Total: $0.00"
                findViewById<LinearLayout>(R.id.layoutTotalesPorTipo).removeAllViews()
            }
        )
        requestQueue.add(request)
    }

    private fun mostrarReporte(reporte: ReporteData) {
        val totalTextView = findViewById<TextView>(R.id.amount15)
        totalTextView.text = "Total: $${String.format("%.2f", reporte.totalGeneral)}"

        val layoutTotales = findViewById<LinearLayout>(R.id.layoutTotalesPorTipo)
        layoutTotales.removeAllViews()

        if (reporte.totalPorTipo.isEmpty()) {
            val noDataTextView = TextView(this).apply {
                text = "No hay datos de ${reporte.tipoMovimiento} para este mes."
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.light_grey))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 16, 0, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            layoutTotales.addView(noDataTextView)
        } else {
            for (tipo in reporte.totalPorTipo) {
                val tipoTextView = TextView(this).apply {
                    text = "${tipo.tipo}: $${String.format("%.2f", tipo.total)}"
                    textSize = 18f
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 16, 0, 16)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(16, 8, 16, 8)
                    }
                }
                layoutTotales.addView(tipoTextView)
            }
        }
    }
}