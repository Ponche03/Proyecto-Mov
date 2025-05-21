package com.example.proyectomov

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class DetalleIngreso : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_ingreso)

        val titulo: TextView = findViewById(R.id.amount8)
        val monto: TextView = findViewById(R.id.amount5)
        val fecha: TextView = findViewById(R.id.amount6)
        val tipo: TextView = findViewById(R.id.amount10)
        val archivo: TextView = findViewById(R.id.amount12)

        intent.extras?.let {
            titulo.text = it.getString("titulo")
            monto.text = it.getString("monto")
            fecha.text = it.getString("fecha")
            tipo.text = it.getString("tipo")
            archivo.text = it.getString("archivo")
        }

    }

}