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
import android.widget.Button // Import Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat // For date formatting
import java.util.Locale // For date formatting

class DetalleIngreso : AppCompatActivity() {

    private var transactionId: String? = null
    private var rawFecha: String? = null // To store the original ISO date

    private fun formatarFechaBonita(fechaISO: String?): String {
        if (fechaISO.isNullOrEmpty()) return "Fecha no disponible"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val date = inputFormat.parse(fechaISO)
            val outputFormat = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES"))
            date?.let { outputFormat.format(it) } ?: fechaISO
        } catch (e: Exception) {
            Log.e("DateParseError", "Error formateando fecha: $fechaISO", e)
            fechaISO.substringBefore("T") // Fallback
        }
    }


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
        val btnEditarIngreso: Button = findViewById(R.id.btnEditarIngreso)

        backArrow.setOnClickListener {
            finish()
        }

        intent.extras?.let { bundle ->
            transactionId = bundle.getString("EXTRA_TRANSACTION_ID")
            val nombre = bundle.getString("nombre", "Detalle del Ingreso")
            val monto = bundle.getString("monto", "0.00")
            rawFecha = bundle.getString("fecha") // Expecting ISO date
            val descripcion = bundle.getString("descripcion", "Sin descripción")
            val tipo = bundle.getString("tipo", "Tipo no especificado")
            val urlArchivo = bundle.getString("archivo")

            tituloTextView.text = nombre
            montoTextView.text = String.format("$%s", monto)
            fechaTextView.text = formatarFechaBonita(rawFecha) // Format for display
            descripcionTextView.text = descripcion
            tipoTextView.text = tipo

            if (!urlArchivo.isNullOrEmpty()) {
                // ... (existing file link logic)
                etiquetaArchivoTextView.visibility = View.VISIBLE
                archivoLinkTextView.visibility = View.VISIBLE

                val textoLink = "Ver archivo adjunto." // Corrected text from original
                val spannableString = SpannableString(textoLink)

                spannableString.setSpan(UnderlineSpan(), 0, textoLink.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlArchivo))
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("DetalleIngreso", "Error al abrir URL: $urlArchivo", e)
                            Toast.makeText(this@DetalleIngreso, "No se puede abrir el enlace.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun updateDrawState(ds: android.text.TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = ContextCompat.getColor(this@DetalleIngreso, R.color.white)
                        ds.isUnderlineText = true
                    }
                }
                spannableString.setSpan(clickableSpan, 0, textoLink.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                archivoLinkTextView.text = spannableString
                archivoLinkTextView.movementMethod = LinkMovementMethod.getInstance()
                // archivoLinkTextView.setTextColor(ContextCompat.getColor(this, R.color.white)) // Already set in updateDrawState

            } else {
                etiquetaArchivoTextView.visibility = View.GONE
                archivoLinkTextView.text = "Sin archivo adjunto."
                archivoLinkTextView.setTextColor(ContextCompat.getColor(this, R.color.light_grey))
                archivoLinkTextView.isClickable = false
            }

            btnEditarIngreso.setOnClickListener {
                if (transactionId != null) {
                    val intent = Intent(this, UpdateIngresoActivity::class.java).apply {
                        putExtra("EXTRA_TRANSACTION_ID", transactionId)
                        putExtra("nombre", nombre)
                        putExtra("monto", monto)
                        putExtra("fecha", rawFecha)
                        putExtra("tipo", tipo)
                        putExtra("descripcion", descripcion)
                        putExtra("archivo", urlArchivo)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "ID de transacción no disponible.", Toast.LENGTH_SHORT).show()
                }
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
            btnEditarIngreso.visibility = View.GONE
        }
    }
}