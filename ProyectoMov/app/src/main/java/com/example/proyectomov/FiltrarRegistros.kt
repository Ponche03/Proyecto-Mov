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


class FiltrarRegistros : AppCompatActivity() {

    private var usuarioID: String = ""
    private var pagina: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filtros)

        usuarioID = UsuarioGlobal.id.toString()

        val textViewFecha = findViewById<TextView>(R.id.title2)

        // Obtener referencias de las flechas
        val rightArrow = findViewById<ImageView>(R.id.right_arrow)
        val leftArrow = findViewById<ImageView>(R.id.left_arrow)

        // Configurar listeners de clic para las flechas
        rightArrow.setOnClickListener {
            pagina++ // Incrementar la página
            actualizarIngresos()
        }

        leftArrow.setOnClickListener {
            if (pagina > 1) { // Asegurar que la página no sea menor que 1
                pagina-- // Decrementar la página
                actualizarIngresos()
            }
        }

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

            pagina = 1 // Reiniciar a la primera página cuando cambia la fecha
            obtenerIngresos(usuarioID, month + 1, year, pagina) // Llamar con mes y año numérico
        }, anio, mes, calendario.get(Calendar.DAY_OF_MONTH))

        datePickerDialog.show()
    }



    private fun obtenerIngresos(usuarioID: String, mes: Int, anio: Int,pagina:Int) {
        val baseUrl = getString(R.string.base_url)
        val url = "$baseUrl/ingresos?usuarioID=$usuarioID&mes=$mes&anio=$anio&limite=2&pagina=$pagina"

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

    private fun actualizarIngresos() {
        // Obtener fecha actual del textViewFecha
        val textViewFecha = findViewById<TextView>(R.id.title2)
        val fechaActual = textViewFecha.text.toString()

        // Extraer mes y año de la fecha actual (ejemplo: "Mayo/2025")
        val partesFecha = fechaActual.split("/")
        val mes = partesFecha[0] // Mes en texto
        val anio = partesFecha[1].toInt() // Año en número

        // Obtener ingresos con la página actualizada
        obtenerIngresos(usuarioID, obtenerNumeroMes(mes), anio, pagina)
    }

    // Función para convertir nombre del mes a número
    private fun obtenerNumeroMes(nombreMes: String): Int {
        val nombresMeses = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        return nombresMeses.indexOf(nombreMes) + 1 // +1 porque los meses en Calendar son 1-indexed
    }

}