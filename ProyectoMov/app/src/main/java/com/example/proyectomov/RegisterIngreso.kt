package com.example.proyectomov

import UsuarioGlobal
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
import services.FirebaseStorageService
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONArray
import org.json.JSONObject

import services.TransactionService


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

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private var currentPhotoPath: String? = null

    private val ingresoFactory = IngresoFactory()
    private val firebaseStorageService = FirebaseStorageService()
    private val transactionService by lazy { TransactionService(this) }


    private val PREF_NAME = "OfflineIngresos"
    private val MAX_REGISTROS = 2




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


        verificarYEnviarIngresosPendientes()


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

    private fun guardarIngresoLocalmente(nombre: String, descripcion: String, tipo: String, monto: Double, archivoUrl: String) {
        val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val ingresosGuardados = sharedPreferences.getString("ingresos", "[]")

        val jsonArray = JSONArray(ingresosGuardados)
        if (jsonArray.length() >= MAX_REGISTROS) {
            jsonArray.remove(0) // Elimina el ingreso más antiguo
        }

        val nuevoIngreso = JSONObject().apply {
            put("nombre", nombre)
            put("descripcion", descripcion)
            put("tipo", tipo)
            put("monto", monto)
            put("archivo", archivoUrl)
        }

        jsonArray.put(nuevoIngreso)

        sharedPreferences.edit().putString("ingresos", jsonArray.toString()).apply()
        Toast.makeText(this, "Ingreso guardado sin conexión.", Toast.LENGTH_SHORT).show()
    }

    private fun verificarYEnviarIngresosPendientes() {
        if (hayConexionInternet()) {
            val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val ingresosGuardados = sharedPreferences.getString("ingresos", "[]")

            val jsonArray = JSONArray(ingresosGuardados)
            for (i in 0 until jsonArray.length()) {
                val ingreso = jsonArray.getJSONObject(i)
                val nuevoIngreso = ingresoFactory.crearTransaccion(
                    usuarioID,
                    ingreso.getString("nombre"),
                    ingreso.getString("descripcion"),
                    obtenerFechaActual(),
                    ingreso.getDouble("monto"),
                    ingreso.getString("tipo"),
                    ""
                )

                transactionService.registrarTransaccion(nuevoIngreso, "ingresos", {
                    Toast.makeText(this, "Ingreso sincronizado con la API.", Toast.LENGTH_SHORT).show()
                }, { errorMessage ->
                    Toast.makeText(this, "Error al sincronizar ingreso: $errorMessage", Toast.LENGTH_LONG).show()
                })

            }

            // Limpia los ingresos después de enviarlos
            sharedPreferences.edit().remove("ingresos").apply()
        }
    }

    private fun hayConexionInternet(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
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

        if (!hayConexionInternet()) {


            if (selectedImageUri != null) {
                firebaseStorageService.uploadFile(
                    fileUri = selectedImageUri!!,
                    storagePath = "ingresos_archivos",
                    onSuccess = { archivoUrl ->
                        guardarIngresoLocalmente(nombre, descripcion, tipo, monto, archivoUrl)
                    },
                    onFailure = { errorMessage ->
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                        btnRegistrarIngreso.isEnabled = true
                    }
                )
            } else {

                guardarIngresoLocalmente(nombre, descripcion, tipo, monto,"")
            }

            return
        }else{

            if (selectedImageUri != null) {
                firebaseStorageService.uploadFile(
                    fileUri = selectedImageUri!!,
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
        val opciones = arrayOf("Seleccionar desde Galería", "Tomar Foto")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Selecciona una opción")
        builder.setItems(opciones) { _, which ->
            when (which) {
                0 -> abrirGaleria()
                1 -> tomarFoto()
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
                selectedImageUri = photoUri
                currentPhotoPath = it.absolutePath
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(intent, TAKE_PHOTO_REQUEST)
            }
        } else {
            Toast.makeText(this, "No se pudo acceder a la cámara.", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Error al crear archivo de imagen.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    selectedImageUri = data?.data
                    selectedImageUri?.let {
                        adjuntarArchivoTextView.text = "Archivo: ${it.lastPathSegment ?: "Imagen seleccionada"}"
                    } ?: run {
                        adjuntarArchivoTextView.text = getString(R.string.adjuntar_Archivo)
                    }
                }
                TAKE_PHOTO_REQUEST -> {
                    adjuntarArchivoTextView.text = "Foto capturada."
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
