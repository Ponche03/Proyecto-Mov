package com.example.proyectomov

import UsuarioGlobal
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

import FactoryMethod.IngresoFactory
import android.widget.ImageView

import Services.FirebaseStorageService
import Services.TransactionService

class RegisterIngreso : AppCompatActivity() {

    private lateinit var nombreIngresoEditText: EditText
    private lateinit var descripcionIngresoEditText: EditText
    private lateinit var tipoIngresoSpinner: Spinner
    private lateinit var montoIngresoEditText: EditText
    private lateinit var adjuntarArchivoTextView: TextView
    private lateinit var btnRegistrarIngreso: Button
    private lateinit var botonRegresar: ImageView

    private var usuarioID: String = ""
    private var selectedImageUri: Uri? = null
    private var selectedFileUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private val PICK_FILE_REQUEST = 3
    private var currentPhotoPath: String? = null

    private val ingresoFactory = IngresoFactory()
    private val firebaseStorageService = FirebaseStorageService()
    private val transactionService by lazy { TransactionService(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro_ingreso)

        usuarioID = UsuarioGlobal.id.toString()

        nombreIngresoEditText = findViewById(R.id.fullname)
        descripcionIngresoEditText = findViewById(R.id.email)
        montoIngresoEditText = findViewById(R.id.monto)
        tipoIngresoSpinner = findViewById(R.id.spinner)
        adjuntarArchivoTextView = findViewById(R.id.profilepicture_text)
        btnRegistrarIngreso = findViewById(R.id.btn_login)
        botonRegresar = findViewById(R.id.imageView)

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.ingreso_categoria_array,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tipoIngresoSpinner.adapter = adapter

        adjuntarArchivoTextView.setOnClickListener {
            mostrarOpcionesDeImagen()
        }

        btnRegistrarIngreso.setOnClickListener {
            handleIngresoRegistration()
        }

        botonRegresar.setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }
    }

    private fun handleIngresoRegistration() {
        val nombre = nombreIngresoEditText.text.toString().trim()
        val descripcion = descripcionIngresoEditText.text.toString().trim()
        val tipo = tipoIngresoSpinner.selectedItem.toString()
        val montoStr = montoIngresoEditText.text.toString().trim()
        val monto = montoStr.toDoubleOrNull()

        if (nombre.isEmpty() || monto == null || monto <= 0) {
            Toast.makeText(this, "Por favor ingresa un nombre válido y monto mayor a 0.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Registrando ingreso...", Toast.LENGTH_SHORT).show()
        btnRegistrarIngreso.isEnabled = false

        if (selectedFileUri != null) {
            firebaseStorageService.uploadFile(
                fileUri = selectedFileUri!!,
                storagePath = "ingresos_archivos",
                onSuccess = { archivoUrl ->
                    registrarIngreso(nombre, descripcion, tipo, monto, archivoUrl)
                },
                onFailure = { errorMessage ->
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    btnRegistrarIngreso.isEnabled = true
                }
            )
        } else {
            registrarIngreso(nombre, descripcion, tipo, monto, "")
        }
    }

    private fun registrarIngreso(nombre: String, descripcion: String, tipo: String, monto: Double, archivoUrl: String) {
        val fecha = obtenerFechaActual()

        val nuevoIngreso = ingresoFactory.crearTransaccion(
            idUser = usuarioID,
            nombre = nombre,
            descripcion = descripcion,
            fecha = fecha,
            monto = monto,
            tipo = tipo,
            archivo = archivoUrl
        )

        transactionService.registrarTransaccion(nuevoIngreso, "ingresos", {
            Toast.makeText(this, "Ingreso registrado correctamente.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }, { errorMessage ->
            Toast.makeText(this, "Error al registrar ingreso: $errorMessage", Toast.LENGTH_LONG).show()
            btnRegistrarIngreso.isEnabled = true
        })
    }

    private fun mostrarOpcionesDeImagen() {
        val opciones = arrayOf("Seleccionar Imagen de Galería", "Tomar Foto", "Seleccionar Archivo")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Selecciona una opción para adjuntar")
        builder.setItems(opciones) { _, which ->
            when (which) {
                0 -> abrirGaleria()
                1 -> tomarFoto()
                2 -> abrirSelectorDeArchivos()
            }
        }
        builder.show()
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*" // Specifically for images
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun tomarFoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile: File? = crearArchivoDeImagen()
            photoFile?.also {
                val photoUri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    it
                )
                selectedFileUri = photoUri
                currentPhotoPath = it.absolutePath
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(intent, TAKE_PHOTO_REQUEST)
            }
        } else {
            Toast.makeText(this, "No se pudo acceder a la cámara.", Toast.LENGTH_SHORT).show()
        }
    }

    // New function to open file selector for any file type
    private fun abrirSelectorDeArchivos() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" // Allows any file type
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(
                Intent.createChooser(intent, "Selecciona un Archivo para Subir"),
                PICK_FILE_REQUEST
            )
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "Por favor, instala un administrador de archivos.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun crearArchivoDeImagen(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        // Ensure image name is unique and has a .jpg extension for photos
        val nombreImagen = "JPEG_${timeStamp}_${UUID.randomUUID()}.jpg"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile(nombreImagen, ".jpg", storageDir).apply {
                currentPhotoPath = absolutePath
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Error al crear archivo de imagen.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    selectedFileUri = data?.data
                    selectedFileUri?.let {
                        adjuntarArchivoTextView.text = "Imagen: ${it.lastPathSegment ?: "seleccionada"}"
                    } ?: run {
                        adjuntarArchivoTextView.text = getString(R.string.adjuntar_Archivo)
                    }
                }
                TAKE_PHOTO_REQUEST -> {

                    adjuntarArchivoTextView.text = "Foto capturada: ${File(currentPhotoPath ?: "").name}"
                }
                PICK_FILE_REQUEST -> {
                    selectedFileUri = data?.data
                    selectedFileUri?.let {
                        val fileName = it.lastPathSegment ?: "Archivo seleccionado."
                        adjuntarArchivoTextView.text = "Archivo: $fileName"
                    } ?: run {
                        adjuntarArchivoTextView.text = getString(R.string.adjuntar_Archivo)
                    }
                }
            }
        }
    }

    private fun obtenerFechaActual(): String {
        val formatoISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        formatoISO.timeZone = TimeZone.getTimeZone("UTC")
        return formatoISO.format(Date())
    }
}
