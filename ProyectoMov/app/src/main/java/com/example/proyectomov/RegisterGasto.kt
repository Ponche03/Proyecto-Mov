package com.example.proyectomov

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
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class RegisterGasto : AppCompatActivity() {

    private lateinit var nombreGasto: EditText
    private lateinit var montoGasto: EditText
    private lateinit var descripcionGasto: EditText
    private lateinit var categoriaSpinner: Spinner
    private lateinit var adjuntarArchivoText: TextView
    private lateinit var btnRegistrar: Button
    private lateinit var clipDePapelImage: ImageView

    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private var currentPhotoPath: String? = null

    private var usuarioID: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro_gasto)

        usuarioID = UsuarioGlobal.id.toString()

        nombreGasto = findViewById(R.id.nombre_gasto)
        montoGasto = findViewById(R.id.monto_gasto)
        descripcionGasto = findViewById(R.id.descripcion_gasto)
        categoriaSpinner = findViewById(R.id.spinner)
        adjuntarArchivoText = findViewById(R.id.profilepicture_text)
        btnRegistrar = findViewById(R.id.btn_registrar)
        clipDePapelImage = findViewById(R.id.imageView5)

        // Llenar spinner con categorías de gasto
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.categorias_gasto_array,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoriaSpinner.adapter = adapter

        adjuntarArchivoText.setOnClickListener {
            mostrarOpcionesDeImagen()
        }

        btnRegistrar.setOnClickListener {
            if (selectedImageUri != null) {
                subirImagenAFirebase(selectedImageUri!!)
            } else {
                registrarGastoEnAPI("")
            }
        }
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
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(intent, TAKE_PHOTO_REQUEST)
            }
        } else {
            Toast.makeText(this, "No se pudo acceder a la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    private fun crearArchivoDeImagen(): File? {
        val nombreImagen = "foto_${UUID.randomUUID()}.jpg"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile(nombreImagen, ".jpg", storageDir).apply {
                currentPhotoPath = absolutePath
            }
        } catch (e: IOException) {
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    selectedImageUri = data?.data
                    adjuntarArchivoText.text = "Imagen seleccionada."
                }
                TAKE_PHOTO_REQUEST -> {
                    adjuntarArchivoText.text = "Foto tomada."
                }
            }
        }
    }

    private fun subirImagenAFirebase(uri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference.child("gastos_archivos/${UUID.randomUUID()}")
        val uploadTask = storageRef.putFile(uri)
        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                registrarGastoEnAPI(downloadUri.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registrarGastoEnAPI(archivoUrl: String) {
        val nombre = nombreGasto.text.toString().trim()
        val montoStr = montoGasto.text.toString().trim()
        val descripcion = descripcionGasto.text.toString().trim()
        val categoria = categoriaSpinner.selectedItem.toString()
        val monto = montoStr.toDoubleOrNull()

        if (nombre.isEmpty() || monto == null || monto <= 0) {
            Toast.makeText(this, "Por favor ingresa un nombre válido y monto mayor a 0", Toast.LENGTH_SHORT).show()
            return
        }

        val fecha = obtenerFechaActual()

        val json = JSONObject()
        json.put("Id_user", usuarioID)
        json.put("Fecha", fecha)
        json.put("Categoria", categoria)
        json.put("Monto", monto)
        json.put("Nombre", nombre)
        json.put("Descripcion", descripcion)
        json.put("Archivo", archivoUrl)  // puede ser "" si no hay imagen

        val requestQueue = Volley.newRequestQueue(this)
        val baseUrl = getString(R.string.base_url)
        val apiUrl = "$baseUrl/gastos/"

        val request = JsonObjectRequest(
            Request.Method.POST, apiUrl, json,
            { response ->
                Toast.makeText(this, "Gasto registrado correctamente", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Dashboard::class.java))
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            })

        requestQueue.add(request)
    }

    private fun obtenerFechaActual(): String {
        val formatoISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        formatoISO.timeZone = TimeZone.getTimeZone("UTC")
        return formatoISO.format(Date())
    }
}
