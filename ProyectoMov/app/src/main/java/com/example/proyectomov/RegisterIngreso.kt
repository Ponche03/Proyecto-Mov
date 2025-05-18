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

class RegisterIngreso : AppCompatActivity() {

    private lateinit var fullname: EditText
    private lateinit var emailEdit: EditText
    private lateinit var spinner: Spinner
    private lateinit var montoEdit: EditText
    private lateinit var archivoText: TextView

    private var usuarioID: String = ""
    private var selectedImageUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro_ingreso)

        usuarioID = UsuarioGlobal.id.toString()

        fullname = findViewById(R.id.fullname)
        emailEdit = findViewById(R.id.email)
        montoEdit = findViewById(R.id.monto)
        spinner = findViewById(R.id.spinner)
        archivoText = findViewById(R.id.profilepicture_text)

        val btnRegisterIngreso = findViewById<Button>(R.id.btn_login)

        // Llenar spinner con opciones
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.ingreso_categoria_array,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Al hacer tap en archivoText, abrir opciones para elegir o tomar foto
        archivoText.setOnClickListener {
            mostrarOpcionesDeImagen()
        }

        btnRegisterIngreso.setOnClickListener {
            if (selectedImageUri != null) {
                subirImagenAFirebase(selectedImageUri!!)
            } else {
                // No hay imagen, enviar registro sin archivo
                registrarIngresoEnAPI("")
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
                    archivoText.text = "Imagen seleccionada."
                }
                TAKE_PHOTO_REQUEST -> {
                    archivoText.text = "Foto tomada."
                }
            }
        }
    }

    private fun subirImagenAFirebase(uri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference.child("ingresos_archivos/${UUID.randomUUID()}")
        val uploadTask = storageRef.putFile(uri)
        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                registrarIngresoEnAPI(downloadUri.toString())
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registrarIngresoEnAPI(archivoUrl: String) {
        val nombre = fullname.text.toString()
        val tipo = spinner.selectedItem.toString()
        val monto = montoEdit.text.toString().toDoubleOrNull() ?: 0.0
        val fecha = obtenerFechaActual()

        val json = JSONObject()
        json.put("Id_user", usuarioID)
        json.put("Fecha", fecha)
        json.put("Tipo", tipo)
        json.put("Monto", monto)
        json.put("Nombre", nombre)
        json.put("Archivo", archivoUrl)  // puede ser "" si no hay foto

        val requestQueue = Volley.newRequestQueue(this)

        val baseUrl = getString(R.string.base_url)
        val apiUrl = "$baseUrl/ingresos/"

        val request = JsonObjectRequest(
            Request.Method.POST, apiUrl, json,
            { response ->
                Toast.makeText(this, "Ingreso registrado correctamente", Toast.LENGTH_SHORT).show()
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
