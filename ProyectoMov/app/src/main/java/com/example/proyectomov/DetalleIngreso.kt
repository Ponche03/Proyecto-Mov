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
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
// import Services.TransactionService // No longer directly used
import androidx.lifecycle.lifecycleScope
import internalStorage.IngresoEntity //
import internalStorage.TransactionRepository //
import kotlinx.coroutines.launch
import java.util.TimeZone

class DetalleIngreso : AppCompatActivity() {

    private var transactionId: String? = null
    private var localIdIngreso: Long = 0L
    private var rawFecha: String? = null

    private val transactionRepository: TransactionRepository by lazy { //
        TransactionRepository(applicationContext)
    }
    private var ingresoEntityForOperation: IngresoEntity? = null

    private lateinit var tituloTextView: TextView
    private lateinit var montoTextView: TextView
    private lateinit var fechaTextView: TextView
    private lateinit var descripcionTextView: TextView
    private lateinit var tipoTextView: TextView
    private lateinit var etiquetaArchivoTextView: TextView
    private lateinit var archivoLinkTextView: TextView
    private lateinit var backArrow: ImageView
    private lateinit var btnEditarIngreso: Button
    private lateinit var btnEliminarIngreso: Button


    private fun formatarFechaBonita(fechaISO: String?): String {
        if (fechaISO.isNullOrEmpty()) return "Fecha no disponible"
        return try {
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
                fechaISO.substringBefore("T") // Fallback
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalle_ingreso) //

        tituloTextView = findViewById(R.id.amount4) //
        montoTextView = findViewById(R.id.amount5) //
        fechaTextView = findViewById(R.id.amount6) //
        descripcionTextView = findViewById(R.id.amount8) //
        tipoTextView = findViewById(R.id.amount10) //
        etiquetaArchivoTextView = findViewById(R.id.amount11) //
        archivoLinkTextView = findViewById(R.id.amount12) //
        backArrow = findViewById(R.id.imageView) //
        btnEditarIngreso = findViewById(R.id.btnEditarIngreso) //
        btnEliminarIngreso = findViewById(R.id.btnEliminarIngreso) //

        backArrow.setOnClickListener {
            finish()
        }

        transactionId = intent.getStringExtra("EXTRA_TRANSACTION_ID")
        localIdIngreso = intent.getLongExtra("EXTRA_LOCAL_ID", 0L)

        lifecycleScope.launch {
            if (transactionId != null) {
                ingresoEntityForOperation = transactionRepository.ingresoDao.getIngresoByServerId(transactionId!!) //
            } else if (localIdIngreso != 0L) {
                ingresoEntityForOperation = transactionRepository.ingresoDao.getIngresoByLocalId(localIdIngreso) //
            }

            if (ingresoEntityForOperation != null) {
                populateUI(ingresoEntityForOperation!!)
            } else {
                populateUIFromIntentFallback()
                Log.w("DetalleIngreso", "No se encontró IngresoEntity en DB, usando datos del Intent.")
                if (transactionId == null && localIdIngreso == 0L) {
                    btnEditarIngreso.visibility = View.GONE
                    btnEliminarIngreso.visibility = View.GONE
                    Toast.makeText(this@DetalleIngreso, "Error: Ingreso no identificable.", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnEditarIngreso.setOnClickListener {
            if (ingresoEntityForOperation != null || transactionId != null) {
                val intent = Intent(this, UpdateIngresoActivity::class.java).apply { //
                    putExtra("EXTRA_TRANSACTION_ID", ingresoEntityForOperation?.transactionId ?: transactionId)
                    putExtra("EXTRA_LOCAL_ID", ingresoEntityForOperation?.localId ?: localIdIngreso)

                    putExtra("nombre", ingresoEntityForOperation?.nombre ?: intent.getStringExtra("nombre"))
                    putExtra("monto", (ingresoEntityForOperation?.monto ?: intent.getStringExtra("monto")?.toDoubleOrNull())?.toString())
                    putExtra("fecha", ingresoEntityForOperation?.fecha ?: intent.getStringExtra("fecha"))
                    putExtra("tipo", ingresoEntityForOperation?.tipo ?: intent.getStringExtra("tipo"))
                    putExtra("descripcion", ingresoEntityForOperation?.descripcion ?: intent.getStringExtra("descripcion"))
                    putExtra("archivo", ingresoEntityForOperation?.archivo ?: intent.getStringExtra("archivo"))
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "No se pueden editar los datos del ingreso.", Toast.LENGTH_SHORT).show()
            }
        }

        btnEliminarIngreso.setOnClickListener {
            if (ingresoEntityForOperation != null) {
                mostrarDialogoConfirmacionEliminarIngreso()
            } else {
                Toast.makeText(this, "No se pueden eliminar los datos del ingreso.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateUI(ingreso: IngresoEntity) { //
        transactionId = ingreso.transactionId
        localIdIngreso = ingreso.localId
        rawFecha = ingreso.fecha

        tituloTextView.text = ingreso.nombre
        montoTextView.text = String.format(Locale.getDefault(), "$%.2f", ingreso.monto)
        fechaTextView.text = formatarFechaBonita(ingreso.fecha)
        descripcionTextView.text = ingreso.descripcion ?: "Sin descripción"
        tipoTextView.text = ingreso.tipo ?: "Tipo no especificado"
        setupArchivoLink(ingreso.archivo)
    }

    private fun populateUIFromIntentFallback() {
        rawFecha = intent.getStringExtra("fecha")
        tituloTextView.text = intent.getStringExtra("nombre") ?: "Detalle del Ingreso"
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

            val textoLink = getString(R.string.Detalle_ingresos_nombre_archivo) //
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
                    ds.color = ContextCompat.getColor(this@DetalleIngreso, R.color.white) //
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


    private fun mostrarDialogoConfirmacionEliminarIngreso() {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar este ingreso? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarIngresoActual()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarIngresoActual() {
        if (ingresoEntityForOperation == null) {
            Toast.makeText(this, "Error: No hay datos del ingreso para eliminar.", Toast.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch {
            try {
                transactionRepository.eliminarIngreso(ingresoEntityForOperation!!) //
                Toast.makeText(this@DetalleIngreso, "Ingreso procesado para eliminación.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@DetalleIngreso, Dashboard::class.java) //
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@DetalleIngreso, "Error al procesar eliminación: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}