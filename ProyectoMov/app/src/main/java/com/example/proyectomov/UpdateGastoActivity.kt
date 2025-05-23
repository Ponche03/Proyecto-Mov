package com.example.proyectomov

import Services.FirebaseStorageService //
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
import internalStorage.GastoEntity //
import internalStorage.NetworkUtils //
import internalStorage.TransactionRepository //
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    private var localIdGasto: Long = 0L
    private var originalFechaISO: String? = null

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private val PICK_FILE_REQUEST = 3

    private val firebaseStorageService = FirebaseStorageService() //
    private val transactionRepository: TransactionRepository by lazy { //
        TransactionRepository(applicationContext)
    }
    private var currentGastoEntity: GastoEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_gasto) //

        transactionId = intent.getStringExtra("EXTRA_TRANSACTION_ID")
        localIdGasto = intent.getLongExtra("EXTRA_LOCAL_ID", 0L)

        nombreGastoEditText = findViewById(R.id.nombre_gasto_update) //
        montoGastoEditText = findViewById(R.id.monto_gasto_update) //
        descripcionGastoEditText = findViewById(R.id.descripcion_gasto_update) //
        categoriaSpinner = findViewById(R.id.spinner_categoria_gasto_update) //
        adjuntarArchivoText = findViewById(R.id.adjuntar_archivo_text_update_gasto) //
        currentFileText = findViewById(R.id.current_file_text_gasto) //
        btnActualizar = findViewById(R.id.btn_actualizar_gasto) //
        botonRegresar = findViewById(R.id.imageViewBackUpdateGasto) //

        val adapter = ArrayAdapter.createFromResource(
            this, R.array.categorias_gasto_array, android.R.layout.simple_spinner_item //
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoriaSpinner.adapter = adapter

        lifecycleScope.launch {
            if (transactionId != null) {
                currentGastoEntity = transactionRepository.gastoDao.getGastoByServerId(transactionId!!) //
            } else if (localIdGasto != 0L) {
                currentGastoEntity = transactionRepository.gastoDao.getGastoByLocalId(localIdGasto) //
            }

            if (currentGastoEntity != null) {
                populateUIFromEntity(currentGastoEntity!!)
            } else {

                Log.w("UpdateGasto", "GastoEntity no encontrada en DB, usando datos del Intent como fallback.")
                populateUIFromIntentFallback()
                if (transactionId == null && localIdGasto == 0L) {
                    Toast.makeText(this@UpdateGastoActivity, "Error: Gasto no identificable para actualizar.", Toast.LENGTH_LONG).show()
                    btnActualizar.isEnabled = false
                }
            }
        }

        adjuntarArchivoText.setOnClickListener { mostrarOpcionesDeArchivo() }
        btnActualizar.setOnClickListener { handleGastoUpdate() }
        botonRegresar.setOnClickListener { finish() }
    }

    private fun populateUIFromEntity(gasto: GastoEntity) { //
        nombreGastoEditText.setText(gasto.nombre)
        montoGastoEditText.setText(gasto.monto.toString())
        descripcionGastoEditText.setText(gasto.descripcion)
        originalFechaISO = gasto.fecha

        val categorias = resources.getStringArray(R.array.categorias_gasto_array) //
        val tipoPosition = categorias.indexOf(gasto.tipo)
        if (tipoPosition >= 0) {
            categoriaSpinner.setSelection(tipoPosition)
        }

        existingFileUrl = gasto.archivo
        if (!existingFileUrl.isNullOrEmpty()) {
            currentFileText.text = "Archivo actual: ${existingFileUrl?.substringAfterLast('/')}"
            currentFileText.visibility = View.VISIBLE //
        } else {
            currentFileText.text = "Archivo actual: ninguno"
            currentFileText.visibility = View.VISIBLE //
        }
    }
    private fun populateUIFromIntentFallback() {
        nombreGastoEditText.setText(intent.getStringExtra("nombre"))
        montoGastoEditText.setText(intent.getStringExtra("monto"))
        descripcionGastoEditText.setText(intent.getStringExtra("descripcion"))
        originalFechaISO = intent.getStringExtra("fecha")

        val tipoGasto = intent.getStringExtra("tipo")
        val categorias = resources.getStringArray(R.array.categorias_gasto_array) //
        val tipoPosition = categorias.indexOf(tipoGasto)
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
        if (originalFechaISO == null){
            Toast.makeText(this, "Error: Fecha original del gasto no disponible.", Toast.LENGTH_LONG).show()
            return
        }

        btnActualizar.isEnabled = false
        Toast.makeText(this, "Actualizando gasto...", Toast.LENGTH_SHORT).show()

        if (selectedFileUri != null) {
            if (NetworkUtils.isNetworkAvailable(this)) { //
                firebaseStorageService.uploadFile( //
                    fileUri = selectedFileUri!!,
                    storagePath = "gastos_archivos",
                    onSuccess = { newFileUrl ->
                        proceedWithUpdateInRepository(nombre, monto, descripcion, categoria, newFileUrl)
                    },
                    onFailure = { errorMsg ->
                        Toast.makeText(this, "Error al subir nuevo archivo: $errorMsg. Guardando con referencia local.", Toast.LENGTH_LONG).show()
                        proceedWithUpdateInRepository(nombre, monto, descripcion, categoria, selectedFileUri.toString()) // Save local URI string
                    }
                )
            } else {
                Toast.makeText(this, "Modo offline. Nuevo archivo se subirá más tarde.", Toast.LENGTH_SHORT).show()
                proceedWithUpdateInRepository(nombre, monto, descripcion, categoria, selectedFileUri.toString()) // Save local URI string
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


        val entidadParaActualizar: GastoEntity //

        if (currentGastoEntity != null) {
            // Modificar la entidad cargada
            entidadParaActualizar = currentGastoEntity!!.copy(
                nombre = nombre,
                monto = monto,
                descripcion = descripcion,
                tipo = categoria,
                archivo = archivoUriOrUrl,
            )
        } else {

            if ((transactionId == null && localIdGasto == 0L) || originalFechaISO == null) {
                Toast.makeText(this, "No se puede actualizar el gasto: faltan identificadores o fecha original.", Toast.LENGTH_LONG).show()
                btnActualizar.isEnabled = true
                return
            }
            entidadParaActualizar = GastoEntity( //
                localId = localIdGasto, // Importante para el DAO si transactionId es null
                transactionId = transactionId,
                idUser = userId,
                nombre = nombre,
                descripcion = descripcion,
                fecha = originalFechaISO!!, // Usar la fecha original guardada
                monto = monto,
                tipo = categoria,
                archivo = archivoUriOrUrl,
                isSynced = false, // El repositorio se encargará
                pendingAction = if (transactionId == null && currentGastoEntity?.pendingAction != "CREATE") "CREATE" else "UPDATE" // Lógica del repositorio
            )
        }

        lifecycleScope.launch {
            try {
                transactionRepository.actualizarGasto(entidadParaActualizar) //
                Toast.makeText(this@UpdateGastoActivity, "Gasto actualizado.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@UpdateGastoActivity, Dashboard::class.java) //
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@UpdateGastoActivity, "Error al actualizar gasto: ${e.message}", Toast.LENGTH_LONG).show()
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