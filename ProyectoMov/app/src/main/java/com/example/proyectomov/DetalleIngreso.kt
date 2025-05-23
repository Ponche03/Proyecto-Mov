package com.example.proyectomov

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class DetalleIngreso : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_ingreso)

        val tituloTextView: TextView = findViewById(R.id.amount4)
        val montoTextView: TextView = findViewById(R.id.amount5)
        val fechaTextView: TextView = findViewById(R.id.amount6)
        val descripcionTextView: TextView = findViewById(R.id.amount8)
        val tipoTextView: TextView = findViewById(R.id.amount10)

        val etiquetaArchivoTextView: TextView = findViewById(R.id.amount11)
        val archivoLinkTextView: TextView = findViewById(R.id.amount12)

        val backArrow: ImageView = findViewById(R.id.imageView)
        backArrow.setOnClickListener {
            finish()
        }

        intent.extras?.let { bundle ->
            tituloTextView.text = bundle.getString("nombre", "Detalle del Ingreso")
            montoTextView.text = String.format("$%s", bundle.getString("monto", "0.00"))
            fechaTextView.text = bundle.getString("fecha", "Fecha no disponible")
            descripcionTextView.text = bundle.getString("descripcion", "Sin descripción")
            tipoTextView.text = bundle.getString("tipo", "Tipo no especificado")

            val urlArchivo = bundle.getString("archivo")

            if (!urlArchivo.isNullOrEmpty()) {
                etiquetaArchivoTextView.visibility = View.VISIBLE
                archivoLinkTextView.visibility = View.VISIBLE

                val textoLink = "Ver archivo adjunto."
                val spannableString = SpannableString(textoLink)

                spannableString.setSpan(UnderlineSpan(), 0, textoLink.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlArchivo))
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("DetalleGasto", "Error al abrir URL: $urlArchivo", e)
                            Toast.makeText(this@DetalleIngreso, "No se puede abrir el enlace.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun updateDrawState(ds: android.text.TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = ContextCompat.getColor(this@DetalleIngreso, R.color.white) // Fuerza el color blanco
                        ds.isUnderlineText = true
                    }
                }

                spannableString.setSpan(clickableSpan, 0, textoLink.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                archivoLinkTextView.text = spannableString
                archivoLinkTextView.movementMethod = LinkMovementMethod.getInstance()
                archivoLinkTextView.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                etiquetaArchivoTextView.visibility = View.GONE
                archivoLinkTextView.text = "Sin archivo adjunto."
                archivoLinkTextView.setTextColor(ContextCompat.getColor(this, R.color.light_grey))
                archivoLinkTextView.isClickable = false
            }
        } ?: run {
            tituloTextView.text = "Detalle del Ingreso"
            montoTextView.text = "$0.00"
            fechaTextView.text = "Fecha no disponible"
            descripcionTextView.text = "Sin descripción"
            tipoTextView.text = "Tipo no especificado"
            etiquetaArchivoTextView.visibility = View.GONE
            archivoLinkTextView.text = "Sin archivo adjunto."
            archivoLinkTextView.isClickable = false
        }
    }
}