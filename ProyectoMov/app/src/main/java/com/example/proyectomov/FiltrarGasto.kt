package com.example.proyectomov

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import com.android.volley.toolbox.JsonObjectRequest
import java.util.Calendar
import android.app.DatePickerDialog
import android.widget.ImageView

class FiltrarGasto : AppCompatActivity() {

    private var usuarioID: String = ""

    private var pagina: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_filtrar_gasto)

        usuarioID = UsuarioGlobal.id.toString()

        val textViewFecha = findViewById<TextView>(R.id.title2)

        textViewFecha.setOnClickListener {
            mostrarDatePicker(textViewFecha)
        }

        // Flechas
        val rightArrow = findViewById<ImageView>(R.id.right_arrow)
        val leftArrow = findViewById<ImageView>(R.id.left_arrow)

        rightArrow.setOnClickListener {
            pagina++
            actualizarGastos()
        }

        leftArrow.setOnClickListener {
            if (pagina > 1) {
                pagina--
                actualizarGastos()
            }
        }
    }

    private fun mostrarDatePicker(textViewFecha: TextView) {
        val calendario = Calendar.getInstance()
        val anio = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, year, month, _ ->
            val nombresMeses = arrayOf(
                "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
            )
            val mesSeleccionado = nombresMeses[month]
            textViewFecha.text = "$mesSeleccionado/$year"

            pagina = 1 // Reiniciar página
            obtenerGasto(usuarioID, month + 1, year, pagina)
        }, anio, mes, calendario.get(Calendar.DAY_OF_MONTH))

        datePickerDialog.show()
    }

    private fun actualizarGastos() {
        val textViewFecha = findViewById<TextView>(R.id.title2)
        val fechaActual = textViewFecha.text.toString()

        if (fechaActual.contains("/")) {
            val partes = fechaActual.split("/")
            val mesTexto = partes[0]
            val anio = partes[1].toInt()
            val mes = obtenerNumeroMes(mesTexto)
            obtenerGasto(usuarioID, mes, anio, pagina)
        }
    }

    private fun obtenerNumeroMes(nombreMes: String): Int {
        val nombresMeses = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        return nombresMeses.indexOf(nombreMes) + 1
    }

    private fun obtenerGasto(usuarioID: String, mes: Int, anio: Int, pagina: Int) {
        val baseUrl = getString(R.string.base_url)
        val url = "$baseUrl/gastos?usuarioID=$usuarioID&mes=$mes&anio=$anio&limite=2&pagina=$pagina"

        val requestQueue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val gastosJsonArray = response.getJSONArray("gastos")
                    val listaGastos = mutableListOf<Gasto>()

                    for (i in 0 until gastosJsonArray.length()) {
                        val item = gastosJsonArray.getJSONObject(i)

                        val gasto = Gasto(
                            nombre = item.getString("Nombre"),
                            fecha = item.getString("Fecha"),
                            tipo = item.getString("Descripcion"),
                            monto = item.getDouble("Monto"),
                            archivo = item.optString("Archivo", "") // Manejo seguro si no hay archivo
                        )
                        listaGastos.add(gasto)
                    }

                    mostrarGasto(listaGastos)

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

    private fun mostrarGasto(lista: List<Gasto>) {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewGasto)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = GastoAdapter(lista)
    }

}