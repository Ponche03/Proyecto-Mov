package com.example.proyectomov

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterIngreso : AppCompatActivity() {

    private lateinit var fullname: EditText
    private lateinit var username: EditText
    private lateinit var emailEdit: EditText
    private lateinit var spinner: Spinner
    private lateinit var montoEdit: EditText
    private lateinit var archivoText: TextView

    private var usuarioID: String = "" // Declarar aquí, inicializar en onCreate
    //private val usuarioID: String = "12345" // Cambia esto por cómo obtienes el ID real
    private val archivoUrl = "https://example.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro_ingreso)

        usuarioID = intent.getStringExtra("usuarioID") ?: ""

        fullname = findViewById(R.id.fullname)
        emailEdit = findViewById(R.id.email)
        montoEdit = findViewById(R.id.monto) // Debe ser el id del EditText de monto
        spinner = findViewById(R.id.spinner)
        archivoText = findViewById(R.id.profilepicture_text)
        val btnRegisterIngreso = findViewById<Button>(R.id.btn_login)

        // Llenar spinner con opciones
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.ingreso_categoria_array,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        btnRegisterIngreso.setOnClickListener {
            registrarIngresoEnAPI()
        }

        // archivoText.setOnClickListener { ... } // Para seleccionar archivo si quieres
    }

    private fun registrarIngresoEnAPI() {
        val nombre = fullname.text.toString()
        val tipo = spinner.selectedItem.toString()
        val monto = montoEdit.text.toString().toDoubleOrNull() ?: 0.0
        val fecha = obtenerFechaActual()

        val json = JSONObject()
        json.put("Id_user", usuarioID)
        json.put("Fecha", fecha)
        json.put("Tipo", tipo)
        json.put("Monto", monto)
        json.put("Nombre", nombre)
        json.put("Archivo", archivoUrl)

        val requestQueue = Volley.newRequestQueue(this)

        val baseUrl = getString(R.string.base_url)
        val apiUrl = "$baseUrl/ingresos/"

        val request = JsonObjectRequest(
            Request.Method.POST, apiUrl, json,
            { response ->
                Toast.makeText(this, "Ingreso registrado correctamente", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LogIn::class.java))
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            })

        requestQueue.add(request)
    }

    private fun obtenerFechaActual(): String {
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formato.format(Date())
    }
}