package com.example.proyectomov

import UsuarioGlobal //
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat // For date formatting
import java.util.Locale // For date formatting
// import Services.TransactionService // No longer directly used
import androidx.lifecycle.lifecycleScope
import internalStorage.GastoEntity //
import internalStorage.TransactionRepository //
import kotlinx.coroutines.launch
import java.util.TimeZone

class DetalleGasto : AppCompatActivity() {

    private var transactionId: String? = null
    private var localIdGasto: Long = 0L // Default to 0, passed from Intent if available
    private var rawFecha: String? = null
    // private val transactionService by lazy { TransactionService(this) } // Replaced by repository
    private val transactionRepository: TransactionRepository by lazy { //
        TransactionRepository(applicationContext)
    }
    private var gastoEntityForOperation: GastoEntity? = null // To store the loaded entity

    private lateinit var tituloTextView: TextView
    private lateinit var montoTextView: TextView
    private lateinit var fechaTextView: TextView
    private lateinit var descripcionTextView: TextView
    private lateinit var tipoTextView: TextView
    private lateinit var etiquetaArchivoTextView: TextView
    private lateinit var archivoLinkTextView: TextView
    private lateinit var backArrow: ImageView
    private lateinit var btnEditarGasto: Button
    private lateinit var btnEliminarGasto: Button


    private fun formatarFechaBonita(fechaISO: String?): String {
        if (fechaISO.isNullOrEmpty()) return "Fecha no disponible"
        return try {
            // Intenta parsear con formato de offset completo primero
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val date = inputFormat.parse(fechaISO)
            val outputFormat = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES")) //
            date?.let { outputFormat.format(it) } ?: fechaISO
        } catch (e: Exception) {
            Log.w("DateParseError", "Primer intento de parseo falló para: $fechaISO. Intentando fallback.", e)
            try {
                // Fallback para formatos sin offset o con 'Z'
                val fallbackFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                fallbackFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = fallbackFormat.parse(fechaISO)
                val outputFormat = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES")) //
                date?.let { outputFormat.format(it) } ?: fechaISO.substringBefore("T")
            } catch (e2: Exception) {
                Log.e("DateParseError", "Error formateando fecha con fallback: $fechaISO", e2)
                fechaISO.substringBefore("T")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_gasto) //

        tituloTextView = findViewById(R.id.amount4) //
        montoTextView = findViewById(R.id.amount5) //
        fechaTextView = findViewById(R.id.amount6) //
        descripcionTextView = findViewById(R.id.amount8) //
        tipoTextView = findViewById(R.id.amount10) //
        etiquetaArchivoTextView = findViewById(R.id.amount11) //
        archivoLinkTextView = findViewById(R.id.amount12) //
        backArrow = findViewById(R.id.imageView) //
        btnEditarGasto = findViewById(R.id.btnEditarGasto) //
        btnEliminarGasto = findViewById(R.id.btnEliminarGasto) //


        backArrow.setOnClickListener {
            finish()
        }

        transactionId = intent.getStringExtra("EXTRA_TRANSACTION_ID")
        localIdGasto = intent.getLongExtra("EXTRA_LOCAL_ID", 0L) // Obtener localId

        lifecycleScope.launch {
            if (transactionId != null) {
                gastoEntityForOperation = transactionRepository.gastoDao.getGastoByServerId(transactionId!!) //
            } else if (localIdGasto != 0L) {
                gastoEntityForOperation = transactionRepository.gastoDao.getGastoByLocalId(localIdGasto) //
            }

            if (gastoEntityForOperation != null) {
                populateUI(gastoEntityForOperation!!)
            } else {

                populateUIFromIntentFallback()
                Log.w("DetalleGasto", "No se encontró GastoEntity en DB, usando datos del Intent.")
                if (transactionId == null && localIdGasto == 0L) {
                    btnEditarGasto.visibility = View.GONE
                    btnEliminarGasto.visibility = View.GONE
                    Toast.makeText(this@DetalleGasto, "Error: Gasto no identificable.", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnEditarGasto.setOnClickListener {
            if (gastoEntityForOperation != null || transactionId != null) { // Allow edit if we have some ID
                val intent = Intent(this, UpdateGastoActivity::class.java).apply {
                    putExtra("EXTRA_TRANSACTION_ID", gastoEntityForOperation?.transactionId ?: transactionId)
                    putExtra("EXTRA_LOCAL_ID", gastoEntityForOperation?.localId ?: localIdGasto) // Pasar localId

                    // Pasar todos los datos necesarios, preferiblemente desde gastoEntityForOperation si está disponible
                    putExtra("nombre", gastoEntityForOperation?.nombre ?: intent.getStringExtra("nombre"))
                    putExtra("monto", (gastoEntityForOperation?.monto ?: intent.getStringExtra("monto")?.toDoubleOrNull())?.toString())
                    putExtra("fecha", gastoEntityForOperation?.fecha ?: intent.getStringExtra("fecha"))
                    putExtra("tipo", gastoEntityForOperation?.tipo ?: intent.getStringExtra("tipo"))
                    putExtra("descripcion", gastoEntityForOperation?.descripcion ?: intent.getStringExtra("descripcion"))
                    putExtra("archivo", gastoEntityForOperation?.archivo ?: intent.getStringExtra("archivo"))
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "No se pueden editar los datos del gasto.", Toast.LENGTH_SHORT).show()
            }
        }

        btnEliminarGasto.setOnClickListener {
            if (gastoEntityForOperation != null) {
                mostrarDialogoConfirmacionEliminar()
            } else {
                Toast.makeText(this, "No se pueden eliminar los datos del gasto.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateUI(gasto: GastoEntity) { //
        transactionId = gasto.transactionId
        localIdGasto = gasto.localId
        rawFecha = gasto.fecha

        tituloTextView.text = gasto.nombre
        montoTextView.text = String.format(Locale.getDefault(), "$%.2f", gasto.monto)
        fechaTextView.text = formatarFechaBonita(gasto.fecha)
        descripcionTextView.text = gasto.descripcion ?: "Sin descripción"
        tipoTextView.text = gasto.tipo ?: "Tipo no especificado"
        setupArchivoLink(gasto.archivo)
    }

    private fun populateUIFromIntentFallback() {

        rawFecha = intent.getStringExtra("fecha")

        tituloTextView.text = intent.getStringExtra("nombre") ?: "Detalle del Gasto"
        val montoStr = intent.getStringExtra("monto") ?: "0.00"
        montoTextView.text = String.format(Locale.getDefault(), "$%s", montoStr)
        fechaTextView.text = formatarFechaBonita(rawFecha)
        descripcionTextView.text = intent.getStringExtra("descripcion") ?: "Sin descripción"
        tipoTextView.text = intent.getStringExtra("tipo") ?: "Tipo no especificado"
        setupArchivoLink(intent.getStringExtra("archivo"))
    }


    private fun setupArchivoLink(urlArchivo: String?) {
        if (!urlArchivo.isNullOrEmpty()) {
            etiquetaArchivoTextView.visibility = View.VISIBLE //
            archivoLinkTextView.visibility = View.VISIBLE //

            val textoLink = getString(R.string.Detalle_gastos_nombre_archivo)
            val spannableString = SpannableString(textoLink)

            spannableString.setSpan(UnderlineSpan(), 0, textoLink.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlArchivo))
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("DetalleGasto", "Error al abrir URL: $urlArchivo", e)
                        Toast.makeText(this@DetalleGasto, "No se puede abrir el enlace.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun updateDrawState(ds: android.text.TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(this@DetalleGasto, R.color.white) //
                    ds.isUnderlineText = true
                }
            }
            spannableString.setSpan(clickableSpan, 0, textoLink.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            archivoLinkTextView.text = spannableString
            archivoLinkTextView.movementMethod = LinkMovementMethod.getInstance()
        } else {
            etiquetaArchivoTextView.visibility = View.GONE //
            archivoLinkTextView.text = "Sin archivo adjunto."
            archivoLinkTextView.setTextColor(ContextCompat.getColor(this, R.color.light_grey)) //
            archivoLinkTextView.isClickable = false
        }
    }

    private fun mostrarDialogoConfirmacionEliminar() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar este gasto? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarGastoActual()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarGastoActual() {
        if (gastoEntityForOperation == null) {
            Toast.makeText(this, "Error: No hay datos del gasto para eliminar.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            try {
                transactionRepository.eliminarGasto(gastoEntityForOperation!!) //
                Toast.makeText(this@DetalleGasto, "Gasto procesado para eliminación.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@DetalleGasto, Dashboard::class.java) //
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@DetalleGasto, "Error al procesar eliminación: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}