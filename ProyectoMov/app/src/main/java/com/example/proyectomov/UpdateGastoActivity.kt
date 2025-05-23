package com.example.proyectomov

import FactoryMethod.GastoFactory
import Services.FirebaseStorageService
import Services.TransactionService
import UsuarioGlobal
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UpdateGastoActivity : AppCompatActivity() {

    private lateinit var nombreGastoEditText: EditText
    private lateinit var montoGastoEditText: EditText
    private lateinit var descripcionGastoEditText: EditText
    private lateinit var categoriaSpinner: Spinner
    private lateinit var adjuntarArchivoText: TextView
    private lateinit var currentFileText: TextView
    private lateinit var btnActualizar: Button
    private lateinit var botonRegresar: ImageView

    private var selectedFileUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var existingFileUrl: String? = null
    private var transactionId: String? = null
    private var originalFechaISO: String? = null

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private val PICK_FILE_REQUEST = 3

    private val gastoFactory = GastoFactory()
    private val transactionService by lazy { TransactionService(this) }
    private val firebaseStorageService = FirebaseStorageService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_gasto)

        transactionId = intent.getStringExtra("EXTRA_TRANSACTION_ID")
        if (transactionId == null) {
            Toast.makeText(this, "Error: ID de transacción no encontrado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        nombreGastoEditText = findViewById(R.id.nombre_gasto_update)
        montoGastoEditText = findViewById(R.id.monto_gasto_update)
        descripcionGastoEditText = findViewById(R.id.descripcion_gasto_update)
        categoriaSpinner = findViewById(R.id.spinner_categoria_gasto_update)
        adjuntarArchivoText = findViewById(R.id.adjuntar_archivo_text_update_gasto)
        currentFileText = findViewById(R.id.current_file_text_gasto)
        btnActualizar = findViewById(R.id.btn_actualizar_gasto)
        botonRegresar = findViewById(R.id.imageViewBackUpdateGasto)

        val adapter = ArrayAdapter.createFromResource(
            this, R.array.categorias_gasto_array, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoriaSpinner.adapter = adapter

        loadExistingData()

        adjuntarArchivoText.setOnClickListener { mostrarOpcionesDeArchivo() }
        btnActualizar.setOnClickListener { handleGastoUpdate() }
        botonRegresar.setOnClickListener { finish() }
    }

    private fun loadExistingData() {
        nombreGastoEditText.setText(intent.getStringExtra("nombre"))
        montoGastoEditText.setText(intent.getStringExtra("monto"))
        descripcionGastoEditText.setText(intent.getStringExtra("descripcion"))
        originalFechaISO = intent.getStringExtra("fecha") // Store original ISO date

        val tipoGasto = intent.getStringExtra("tipo")
        val categorias = resources.getStringArray(R.array.categorias_gasto_array)
        val tipoPosition = categorias.indexOf(tipoGasto)
        if (tipoPosition >= 0) {
            categoriaSpinner.setSelection(tipoPosition)
        }

        existingFileUrl = intent.getStringExtra("archivo")
        if (!existingFileUrl.isNullOrEmpty()) {
            currentFileText.text = "Archivo actual: ${existingFileUrl?.substringAfterLast('/')}"
            currentFileText.visibility = View.VISIBLE
        } else {
            currentFileText.text = "Archivo actual: ninguno"
            currentFileText.visibility = View.VISIBLE
        }
    }

    private fun handleGastoUpdate() {
        val nombre = nombreGastoEditText.text.toString().trim()
        val montoStr = montoGastoEditText.text.toString().trim()
        val descripcion = descripcionGastoEditText.text.toString().trim()
        val categoria = categoriaSpinner.selectedItem.toString()
        val monto = montoStr.toDoubleOrNull()

        if (nombre.isEmpty() || monto == null || monto <= 0) {
            Toast.makeText(this, "Nombre y monto (mayor a 0) son requeridos.", Toast.LENGTH_SHORT).show()
            return
        }

        btnActualizar.isEnabled = false
        Toast.makeText(this, "Actualizando gasto...", Toast.LENGTH_SHORT).show()

        if (selectedFileUri != null) {
            firebaseStorageService.uploadFile(
                fileUri = selectedFileUri!!,
                storagePath = "gastos_archivos",
                onSuccess = { newFileUrl ->
                    actualizarGastoEnServidor(nombre, monto, descripcion, categoria, newFileUrl)
                },
                onFailure = { errorMsg ->
                    Toast.makeText(this, "Error al subir nuevo archivo: $errorMsg", Toast.LENGTH_LONG).show()
                    btnActualizar.isEnabled = true
                }
            )
        } else {
            actualizarGastoEnServidor(nombre, monto, descripcion, categoria, existingFileUrl)
        }
    }

    private fun actualizarGastoEnServidor(nombre: String, monto: Double, descripcion: String, categoria: String, archivoUrl: String?) {
        val userId = UsuarioGlobal.id ?: ""
        if (userId.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no identificado.", Toast.LENGTH_LONG).show()
            btnActualizar.isEnabled = true
            return
        }
        if (originalFechaISO == null) {
            Toast.makeText(this, "Error: Fecha original no disponible.", Toast.LENGTH_LONG).show()
            btnActualizar.isEnabled = true
            return
        }

        val gastoActualizado = gastoFactory.crearTransaccion(
            transactionId = transactionId!!, // Already checked not null
            idUser = userId,
            nombre = nombre,
            descripcion = descripcion,
            fecha = originalFechaISO!!, // Send original ISO date, API should not update it unless explicitly designed to
            monto = monto,
            tipo = categoria,
            archivo = archivoUrl
        )

        transactionService.actualizarTransaccion(transactionId!!, gastoActualizado, "gastos",
            { response ->
                Toast.makeText(this, "Gasto actualizado: ${response.optString("message", "")}", Toast.LENGTH_SHORT).show()
                // Navigate back or refresh dashboard
                val intent = Intent(this, Dashboard::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            },
            { errorMsg ->
                Toast.makeText(this, "Error al actualizar gasto: $errorMsg", Toast.LENGTH_LONG).show()
                btnActualizar.isEnabled = true
            }
        )
    }


    private fun mostrarOpcionesDeArchivo() {
        val opciones = arrayOf("Seleccionar Imagen de Galería", "Tomar Foto", "Seleccionar Archivo", "Quitar Archivo Actual")
        AlertDialog.Builder(this)
            .setTitle("Adjuntar archivo")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> abrirGaleria()
                    1 -> tomarFoto()
                    2 -> abrirSelectorDeArchivos()
                    3 -> {
                        selectedFileUri = null // Clear any newly selected file
                        existingFileUrl = null // Mark that we want to remove the existing file (or send empty string to API)
                        adjuntarArchivoText.text = getString(R.string.adjuntar_Archivo)
                        currentFileText.text = "Archivo actual: ninguno (se quitará)"
                        Toast.makeText(this, "Archivo actual se quitará al actualizar.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun tomarFoto() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    crearArchivoDeImagen()
                } catch (ex: IOException) {
                    Toast.makeText(this, "Error al crear archivo de imagen.", Toast.LENGTH_SHORT).show()
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", it)
                    selectedFileUri = photoURI
                    currentPhotoPath = it.absolutePath
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, TAKE_PHOTO_REQUEST)
                }
            }
        }
    }

    private fun abrirSelectorDeArchivos() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            startActivityForResult(Intent.createChooser(intent, "Selecciona un Archivo"), PICK_FILE_REQUEST)
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "Instala un administrador de archivos.", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun crearArchivoDeImagen(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val uri: Uri? = when (requestCode) {
                PICK_IMAGE_REQUEST, PICK_FILE_REQUEST -> data?.data
                TAKE_PHOTO_REQUEST -> selectedFileUri
                else -> null
            }
            uri?.let {
                selectedFileUri = it
                val fileName = it.lastPathSegment ?: "archivo seleccionado"
                adjuntarArchivoText.text = "Nuevo archivo: $fileName"
                currentFileText.text = "Archivo actual: (se reemplazará con el nuevo)"
            }
        }
    }
}