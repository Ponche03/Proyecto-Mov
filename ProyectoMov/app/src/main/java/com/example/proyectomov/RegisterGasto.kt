package com.example.proyectomov

import UsuarioGlobal //
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

import FactoryMethod.GastoFactory //
import Services.TransactionService // Not directly used, but repository uses it.
import Services.FirebaseStorageService //
import androidx.lifecycle.lifecycleScope
import internalStorage.GastoEntity //
import internalStorage.NetworkUtils //
import internalStorage.TransactionRepository //
import kotlinx.coroutines.launch


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

    private val gastoFactory = GastoFactory() //
    // private val transactionService by lazy { TransactionService(this) } // Now using repository
    private val firebaseStorageService = FirebaseStorageService() //
    private val transactionRepository: TransactionRepository by lazy { //
        TransactionRepository(applicationContext)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro_gasto) //


        usuarioID = UsuarioGlobal.id ?: run { //
            Toast.makeText(this, "Error: Usuario no identificado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }


        nombreGasto = findViewById(R.id.nombre_gasto) //
        montoGasto = findViewById(R.id.monto_gasto) //
        descripcionGasto = findViewById(R.id.descripcion_gasto) //
        categoriaSpinner = findViewById(R.id.spinner) //
        adjuntarArchivoText = findViewById(R.id.profilepicture_text) //
        btnRegistrar = findViewById(R.id.btn_registrar) //
        botonRegresar = findViewById(R.id.imageView) //

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.categorias_gasto_array, //
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
            // Navigate back to Dashboard or appropriate screen
            // startActivity(Intent(this, Dashboard::class.java))
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

        btnRegistrar.isEnabled = false
        Toast.makeText(this, "Registrando gasto...", Toast.LENGTH_SHORT).show()

        if (selectedFileUri != null) {
            if (NetworkUtils.isNetworkAvailable(this)) { //
                firebaseStorageService.uploadFile( //
                    fileUri = selectedFileUri!!,
                    storagePath = "gastos_archivos",
                    onSuccess = { archivoUrl -> // This is the remote URL
                        proceedToSaveTransaction(nombre, monto, descripcion, categoria, archivoUrl)
                    },
                    onFailure = { errorMessage ->
                        Toast.makeText(this, "Error al subir archivo: $errorMessage. Guardando con referencia local.", Toast.LENGTH_LONG).show()
                        // Save with local URI string if Firebase upload fails despite network being "available" initially
                        proceedToSaveTransaction(nombre, monto, descripcion, categoria, selectedFileUri.toString())
                    }
                )
            } else { // Network is not available, save with local URI string
                Toast.makeText(this, "Modo offline. Archivo se subirá más tarde.", Toast.LENGTH_SHORT).show()
                proceedToSaveTransaction(nombre, monto, descripcion, categoria, selectedFileUri.toString())
            }
        } else { // No file selected
            proceedToSaveTransaction(nombre, monto, descripcion, categoria, null)
        }
    }

    private fun proceedToSaveTransaction(nombre: String, monto: Double, descripcion: String, categoria: String, archivoUriOrUrl: String?) {
        val fecha = obtenerFechaActual()

        // Create GastoEntity directly
        val nuevoGastoEntity = GastoEntity( //
            transactionId = null, // Will be set by repository if API call is successful
            idUser = usuarioID,
            nombre = nombre,
            descripcion = descripcion,
            fecha = fecha,
            monto = monto,
            tipo = categoria,
            archivo = archivoUriOrUrl, // This can be a remote URL or a local content URI string
            isSynced = false, // Will be updated by repository
            pendingAction = "CREATE" // Will be updated by repository
        )

        lifecycleScope.launch {
            try {
                transactionRepository.registrarGasto(nuevoGastoEntity) //
                Toast.makeText(this@RegisterGasto, "Gasto registrado.", Toast.LENGTH_SHORT).show()
                // Navigate back to Dashboard or refresh
                val intent = Intent(this@RegisterGasto, Dashboard::class.java) //
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@RegisterGasto, "Error al guardar gasto: ${e.message}", Toast.LENGTH_LONG).show()
                btnRegistrar.isEnabled = true
            }
        }
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
                    "${applicationContext.packageName}.provider", //
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
                        adjuntarArchivoText.text = getString(R.string.adjuntar_Archivo) //
                    }
                }
                TAKE_PHOTO_REQUEST -> {
                    // selectedFileUri is already set in tomarFoto()
                    adjuntarArchivoText.text = "Foto capturada: ${File(currentPhotoPath ?: "").name}"
                }
                PICK_FILE_REQUEST -> {
                    selectedFileUri = data?.data
                    selectedFileUri?.let {
                        val fileName = it.lastPathSegment ?: "Archivo seleccionado."
                        adjuntarArchivoText.text = "Archivo: $fileName"
                    } ?: run {
                        adjuntarArchivoText.text = getString(R.string.adjuntar_Archivo) //
                    }
                }
            }
        }
    }

    private fun obtenerFechaActual(): String {
        val formatoISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        formatoISO.timeZone = TimeZone.getTimeZone("UTC")
        return formatoISO.format(Date())
    }
}