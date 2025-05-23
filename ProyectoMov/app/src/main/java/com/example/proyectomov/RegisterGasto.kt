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
import android.widget.ImageView
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

import FactoryMethod.GastoFactory
import Services.TransactionService
import Services.FirebaseStorageService


class RegisterGasto : AppCompatActivity() {

    private lateinit var nombreGasto: EditText
    private lateinit var montoGasto: EditText
    private lateinit var descripcionGasto: EditText
    private lateinit var categoriaSpinner: Spinner
    private lateinit var adjuntarArchivoText: TextView
    private lateinit var btnRegistrar: Button
    private lateinit var botonRegresar: ImageView


    private var selectedFileUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private val PICK_FILE_REQUEST = 3
    private var currentPhotoPath: String? = null

    private var usuarioID: String = ""

    private val gastoFactory = GastoFactory()
    private val transactionService by lazy { TransactionService(this) }
    private val firebaseStorageService = FirebaseStorageService()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro_gasto)


        usuarioID = UsuarioGlobal.id ?: run {
            Toast.makeText(this, "Error: Usuario no identificado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }


        nombreGasto = findViewById(R.id.nombre_gasto)
        montoGasto = findViewById(R.id.monto_gasto)
        descripcionGasto = findViewById(R.id.descripcion_gasto)
        categoriaSpinner = findViewById(R.id.spinner)
        adjuntarArchivoText = findViewById(R.id.profilepicture_text)
        btnRegistrar = findViewById(R.id.btn_registrar)
        botonRegresar = findViewById(R.id.imageView)

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.categorias_gasto_array,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoriaSpinner.adapter = adapter

        adjuntarArchivoText.setOnClickListener {
            mostrarOpcionesDeArchivo()
        }

        btnRegistrar.setOnClickListener {
            handleGastoRegistration()
        }

        botonRegresar.setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }
    }

    private fun handleGastoRegistration() {
        val nombre = nombreGasto.text.toString().trim()
        val montoStr = montoGasto.text.toString().trim()
        val descripcion = descripcionGasto.text.toString().trim()
        val categoria = categoriaSpinner.selectedItem.toString()
        val monto = montoStr.toDoubleOrNull()

        if (nombre.isEmpty() || monto == null || monto <= 0) {
            Toast.makeText(this, "Por favor ingresa un nombre válido y monto mayor a 0", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Registrando gasto...", Toast.LENGTH_SHORT).show()
        btnRegistrar.isEnabled = false

        if (selectedFileUri != null) {
            firebaseStorageService.uploadFile(
                fileUri = selectedFileUri!!,
                storagePath = "gastos_archivos",
                onSuccess = { archivoUrl ->
                    registrarGasto(nombre, monto, descripcion, categoria, archivoUrl)
                },
                onFailure = { errorMessage ->
                    Toast.makeText(this, "Error al subir archivo: $errorMessage", Toast.LENGTH_LONG).show()
                    btnRegistrar.isEnabled = true
                }
            )
        } else {
            registrarGasto(nombre, monto, descripcion, categoria, "")
        }
    }

    private fun registrarGasto(nombre: String, monto: Double, descripcion: String, categoria: String, archivoUrl: String) {
        val fecha = obtenerFechaActual()

        val nuevoGasto = gastoFactory.crearTransaccion(
            idUser = usuarioID,
            nombre = nombre,
            descripcion = descripcion,
            fecha = fecha,
            monto = monto,
            tipo = categoria,
            archivo = archivoUrl
        )

        transactionService.registrarTransaccion(nuevoGasto, "gastos", {
            Toast.makeText(this, "Gasto registrado correctamente.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }, { errorMessage ->
            Toast.makeText(this, "Error al registrar el gasto: $errorMessage", Toast.LENGTH_LONG).show()
            btnRegistrar.isEnabled = true
        })
    }

    private fun mostrarOpcionesDeArchivo() {
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
        intent.type = "image/*"
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
            Toast.makeText(this, "No se pudo acceder a la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirSelectorDeArchivos() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
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
        val nombreImagen = "JPEG_${timeStamp}_${UUID.randomUUID()}.jpg"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile(nombreImagen, ".jpg", storageDir).apply {
                currentPhotoPath = absolutePath
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show()
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
                        adjuntarArchivoText.text = "Imagen: ${it.lastPathSegment ?: "seleccionada"}"
                    } ?: run {
                        adjuntarArchivoText.text = getString(R.string.adjuntar_Archivo)
                    }
                }
                TAKE_PHOTO_REQUEST -> {
                    adjuntarArchivoText.text = "Foto capturada: ${File(currentPhotoPath ?: "").name}"
                }
                PICK_FILE_REQUEST -> {
                    selectedFileUri = data?.data
                    selectedFileUri?.let {
                        val fileName = it.lastPathSegment ?: "Archivo seleccionado."
                        adjuntarArchivoText.text = "Archivo: $fileName"
                    } ?: run {
                        adjuntarArchivoText.text = getString(R.string.adjuntar_Archivo)
                    }
                }
            }
        }
    }

    private fun obtenerFechaActual(): String {
        // Formato ISO 8601 para la fecha, estándar para APIs
        val formatoISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        formatoISO.timeZone = TimeZone.getTimeZone("UTC") // Usar UTC para consistencia
        return formatoISO.format(Date())
    }
}
