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


class FiltrarRegistros : AppCompatActivity() {

    private var usuarioID: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filtros)

        usuarioID = UsuarioGlobal.id.toString()

        val textViewFecha = findViewById<TextView>(R.id.title2)

        textViewFecha.setOnClickListener {
            mostrarDatePicker(textViewFecha)
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
            val mesSeleccionado = nombresMeses[month] // Obtener el nombre del mes
            textViewFecha.text = "$mesSeleccionado/$year" // Actualizar el texto del TextView
            obtenerIngresos(usuarioID, month + 1, year) // Pasar el número del mes a la función
        }, anio, mes, calendario.get(Calendar.DAY_OF_MONTH))

        datePickerDialog.show()
    }



    private fun obtenerIngresos(usuarioID: String, mes: Int, anio: Int) {
        val baseUrl = getString(R.string.base_url)
        val url = "$baseUrl/ingresos?usuarioID=$usuarioID&mes=$mes&anio=$anio&limite=4&pagina=1"

        val requestQueue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val ingresosJsonArray = response.getJSONArray("ingresos")
                    val listaIngresos = mutableListOf<Ingreso>()

                    for (i in 0 until ingresosJsonArray.length()) {
                        val item = ingresosJsonArray.getJSONObject(i)

                        val ingreso = Ingreso(
                            nombre = item.getString("Nombre"),
                            fecha = item.getString("FechaLocal"),
                            tipo = item.getString("Tipo"),
                            monto = item.getDouble("Monto"),
                            archivo = item.getString("Archivo")
                        )
                        listaIngresos.add(ingreso)
                    }

                    mostrarIngresos(listaIngresos)

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

    private fun mostrarIngresos(lista: List<Ingreso>) {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewIngresos)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = IngresoAdapter(lista)
    }

}