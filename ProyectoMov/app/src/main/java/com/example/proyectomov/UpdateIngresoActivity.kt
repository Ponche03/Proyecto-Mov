package com.example.proyectomov

import Services.FirebaseStorageService //
// import Services.TransactionService // No se usa directamente
import UsuarioGlobal //
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import internalStorage.IngresoEntity //
import internalStorage.NetworkUtils //
import internalStorage.TransactionRepository //
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class UpdateIngresoActivity : AppCompatActivity() {

    private lateinit var nombreIngresoEditText: EditText
    private lateinit var montoIngresoEditText: EditText
    private lateinit var descripcionIngresoEditText: EditText
    private lateinit var categoriaSpinner: Spinner
    private lateinit var adjuntarArchivoText: TextView
    private lateinit var currentFileText: TextView
    private lateinit var btnActualizar: Button
    private lateinit var botonRegresar: ImageView

    private var selectedFileUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var existingFileUrl: String? = null
    private var transactionId: String? = null
    private var localIdIngreso: Long = 0L
    private var originalFechaISO: String? = null

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private val PICK_FILE_REQUEST = 3

    private val firebaseStorageService = FirebaseStorageService() //
    private val transactionRepository: TransactionRepository by lazy { //
        TransactionRepository(applicationContext)
    }
    private var currentIngresoEntity: IngresoEntity? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_ingreso) //

        transactionId = intent.getStringExtra("EXTRA_TRANSACTION_ID")
        localIdIngreso = intent.getLongExtra("EXTRA_LOCAL_ID", 0L)


        nombreIngresoEditText = findViewById(R.id.nombre_ingreso_update) //
        montoIngresoEditText = findViewById(R.id.monto_ingreso_update) //
        descripcionIngresoEditText = findViewById(R.id.descripcion_ingreso_update) //
        categoriaSpinner = findViewById(R.id.spinner_categoria_ingreso_update) //
        adjuntarArchivoText = findViewById(R.id.adjuntar_archivo_text_update_ingreso) //
        currentFileText = findViewById(R.id.current_file_text_ingreso) //
        btnActualizar = findViewById(R.id.btn_actualizar_ingreso) //
        botonRegresar = findViewById(R.id.imageViewBackUpdateIngreso) //

        val adapter = ArrayAdapter.createFromResource(
            this, R.array.ingreso_categoria_array, android.R.layout.simple_spinner_item //
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoriaSpinner.adapter = adapter

        lifecycleScope.launch{
            if (transactionId != null) {
                currentIngresoEntity = transactionRepository.ingresoDao.getIngresoByServerId(transactionId!!) //
            } else if (localIdIngreso != 0L) {
                currentIngresoEntity = transactionRepository.ingresoDao.getIngresoByLocalId(localIdIngreso) //
            }

            if (currentIngresoEntity != null) {
                populateUIFromEntity(currentIngresoEntity!!)
            } else {
                Log.w("UpdateIngreso", "IngresoEntity no encontrada en DB, usando datos del Intent como fallback.")
                populateUIFromIntentFallback()
                if (transactionId == null && localIdIngreso == 0L) {
                    Toast.makeText(this@UpdateIngresoActivity, "Error: Ingreso no identificable para actualizar.", Toast.LENGTH_LONG).show()
                    btnActualizar.isEnabled = false
                }
            }
        }

        adjuntarArchivoText.setOnClickListener { mostrarOpcionesDeArchivo() }
        btnActualizar.setOnClickListener { handleIngresoUpdate() }
        botonRegresar.setOnClickListener { finish() }
    }

    private fun populateUIFromEntity(ingreso: IngresoEntity) { //
        nombreIngresoEditText.setText(ingreso.nombre)
        montoIngresoEditText.setText(ingreso.monto.toString())
        descripcionIngresoEditText.setText(ingreso.descripcion)
        originalFechaISO = ingreso.fecha // Keep original date

        val categorias = resources.getStringArray(R.array.ingreso_categoria_array) //
        val tipoPosition = categorias.indexOf(ingreso.tipo)
        if (tipoPosition >= 0) {
            categoriaSpinner.setSelection(tipoPosition)
        }

        existingFileUrl = ingreso.archivo
        if (!existingFileUrl.isNullOrEmpty()) {
            currentFileText.text = "Archivo actual: ${existingFileUrl?.substringAfterLast('/')}"
            currentFileText.visibility = View.VISIBLE //
        } else {
            currentFileText.text = "Archivo actual: ninguno"
            currentFileText.visibility = View.VISIBLE //
        }
    }

    private fun populateUIFromIntentFallback() {
        nombreIngresoEditText.setText(intent.getStringExtra("nombre"))
        montoIngresoEditText.setText(intent.getStringExtra("monto"))
        descripcionIngresoEditText.setText(intent.getStringExtra("descripcion"))
        originalFechaISO = intent.getStringExtra("fecha")

        val tipoIngreso = intent.getStringExtra("tipo")
        val categorias = resources.getStringArray(R.array.ingreso_categoria_array) //
        val tipoPosition = categorias.indexOf(tipoIngreso)
        if (tipoPosition >= 0) {
            categoriaSpinner.setSelection(tipoPosition)
        }

        existingFileUrl = intent.getStringExtra("archivo")
        if (!existingFileUrl.isNullOrEmpty()) {
            currentFileText.text = "Archivo actual: ${existingFileUrl?.substringAfterLast('/')}"
            currentFileText.visibility = View.VISIBLE //
        } else {
            currentFileText.text = "Archivo actual: ninguno"
            currentFileText.visibility = View.VISIBLE //
        }
    }

    private fun handleIngresoUpdate() {
        val nombre = nombreIngresoEditText.text.toString().trim()
        val montoStr = montoIngresoEditText.text.toString().trim()
        val descripcion = descripcionIngresoEditText.text.toString().trim()
        val categoria = categoriaSpinner.selectedItem.toString()
        val monto = montoStr.toDoubleOrNull()

        if (nombre.isEmpty() || monto == null || monto <= 0) {
            Toast.makeText(this, "Nombre y monto (mayor a 0) son requeridos.", Toast.LENGTH_SHORT).show()
            return
        }
        if (originalFechaISO == null){
            Toast.makeText(this, "Error: Fecha original del ingreso no disponible.", Toast.LENGTH_LONG).show()
            return
        }


        btnActualizar.isEnabled = false
        Toast.makeText(this, "Actualizando ingreso...", Toast.LENGTH_SHORT).show()

        if (selectedFileUri != null) {
            if (NetworkUtils.isNetworkAvailable(this)) { //
                firebaseStorageService.uploadFile( //
                    fileUri = selectedFileUri!!,
                    storagePath = "ingresos_archivos",
                    onSuccess = { newFileUrl ->
                        proceedWithUpdateInRepository(nombre, monto, descripcion, categoria, newFileUrl)
                    },
                    onFailure = { errorMsg ->
                        Toast.makeText(this, "Error al subir nuevo archivo: $errorMsg. Guardando con referencia local.", Toast.LENGTH_LONG).show()
                        proceedWithUpdateInRepository(nombre, monto, descripcion, categoria, selectedFileUri.toString())
                    }
                )
            } else {
                Toast.makeText(this, "Modo offline. Nuevo archivo se subirá más tarde.", Toast.LENGTH_SHORT).show()
                proceedWithUpdateInRepository(nombre, monto, descripcion, categoria, selectedFileUri.toString())
            }
        } else {
            proceedWithUpdateInRepository(nombre, monto, descripcion, categoria, existingFileUrl)
        }
    }

    private fun proceedWithUpdateInRepository(nombre: String, monto: Double, descripcion: String, categoria: String, archivoUriOrUrl: String?) {
        val userId = UsuarioGlobal.id ?: "" //
        if (userId.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no identificado.", Toast.LENGTH_LONG).show()
            btnActualizar.isEnabled = true
            return
        }
        if (this.originalFechaISO == null) {
            Toast.makeText(this, "Error crítico: Fecha original no disponible para la actualización.", Toast.LENGTH_LONG).show()
            btnActualizar.isEnabled = true
            return
        }

        val entidadParaActualizar: IngresoEntity //

        if (currentIngresoEntity != null) {
            entidadParaActualizar = currentIngresoEntity!!.copy(
                nombre = nombre,
                monto = monto,
                descripcion = descripcion,
                tipo = categoria,
                archivo = archivoUriOrUrl
            )
        } else {
            if ((transactionId == null && localIdIngreso == 0L) || originalFechaISO == null) {
                Toast.makeText(this, "No se puede actualizar el ingreso: faltan identificadores o fecha original.", Toast.LENGTH_LONG).show()
                btnActualizar.isEnabled = true
                return
            }
            entidadParaActualizar = IngresoEntity( //
                localId = localIdIngreso,
                transactionId = transactionId,
                idUser = userId,
                nombre = nombre,
                descripcion = descripcion,
                fecha = originalFechaISO!!,
                monto = monto,
                tipo = categoria,
                archivo = archivoUriOrUrl,
                isSynced = false,
                pendingAction = if (transactionId == null && currentIngresoEntity?.pendingAction != "CREATE") "CREATE" else "UPDATE"
            )
        }

        lifecycleScope.launch {
            try {
                transactionRepository.actualizarIngreso(entidadParaActualizar) //
                Toast.makeText(this@UpdateIngresoActivity, "Ingreso actualizado.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@UpdateIngresoActivity, Dashboard::class.java) //
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@UpdateIngresoActivity, "Error al actualizar ingreso: ${e.message}", Toast.LENGTH_LONG).show()
                btnActualizar.isEnabled = true
            }
        }
    }

    private fun mostrarOpcionesDeArchivo() {
        val opciones = mutableListOf("Seleccionar Imagen de Galería", "Tomar Foto", "Seleccionar Archivo")
        if (existingFileUrl != null || selectedFileUri != null) {
            opciones.add("Quitar Archivo Actual")
        }
        AlertDialog.Builder(this)
            .setTitle("Adjuntar archivo")
            .setItems(opciones.toTypedArray()) { _, which ->
                when (opciones[which]) {
                    "Seleccionar Imagen de Galería" -> abrirGaleria()
                    "Tomar Foto" -> tomarFoto()
                    "Seleccionar Archivo" -> abrirSelectorDeArchivos()
                    "Quitar Archivo Actual" -> {
                        selectedFileUri = null
                        existingFileUrl = null
                        adjuntarArchivoText.text = getString(R.string.adjuntar_Archivo) //
                        currentFileText.text = "Archivo actual: ninguno (se quitará al actualizar)"
                        Toast.makeText(this, "El archivo se quitará al actualizar.", Toast.LENGTH_SHORT).show()
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
                    val photoURI: Uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", it) //
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
        return File.createTempFile("JPEG_${timeStamp}_${UUID.randomUUID()}", ".jpg", storageDir).apply {
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
                adjuntarArchivoText.text = getString(R.string.adjuntar_Archivo) //
                currentFileText.text = "Nuevo archivo: $fileName (reemplazará al actual si existe)"
                currentFileText.visibility = View.VISIBLE //
            }
        }
    }
}