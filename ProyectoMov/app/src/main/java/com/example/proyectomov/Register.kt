package com.example.proyectomov

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import services.FirebaseStorageService
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class Register : AppCompatActivity() {

    private lateinit var fullnameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var uploadPictureTextView: TextView
    private lateinit var btnRegisterUser: Button
    private lateinit var loginLinkTextView: TextView

    private var selectedImageUri: Uri? = null

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private var currentPhotoPath: String? = null

    private val firebaseStorageService = FirebaseStorageService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro_usuario)

        fullnameEditText = findViewById(R.id.fullname)
        usernameEditText = findViewById(R.id.username)
        emailEditText = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.password)
        uploadPictureTextView = findViewById(R.id.profilepicture_text)
        btnRegisterUser = findViewById(R.id.btn_login)
        loginLinkTextView = findViewById(R.id.register_link)

        loginLinkTextView.setOnClickListener {
            startActivity(Intent(this, LogIn::class.java))
        }

        uploadPictureTextView.setOnClickListener {
            mostrarOpcionesDeImagen()
        }

        btnRegisterUser.setOnClickListener {
            handleUserRegistration()
        }
    }

    private fun handleUserRegistration() {
        val nombre = fullnameEditText.text.toString().trim()
        val usuario = usernameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (nombre.isEmpty() || usuario.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Selecciona una imagen de perfil.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Registrando usuario...", Toast.LENGTH_SHORT).show()
        btnRegisterUser.isEnabled = false

        firebaseStorageService.uploadFile(
            fileUri = selectedImageUri!!,
            storagePath = "perfil_imagenes",
            onSuccess = { imageUrl ->
                registrarUsuarioEnAPI(nombre, usuario, email, password, imageUrl)
            },
            onFailure = { errorMessage ->
                Toast.makeText(this, "Error al subir imagen: $errorMessage", Toast.LENGTH_LONG).show()
                btnRegisterUser.isEnabled = true
            }
        )
    }

    private fun mostrarOpcionesDeImagen() {
        val opciones = arrayOf("Seleccionar desde Galería", "Tomar Foto")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Selecciona una opción para tu foto de perfil")
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
                        uploadPictureTextView.text = "Imagen: ${it.lastPathSegment ?: "seleccionada"}"
                    }
                }
                TAKE_PHOTO_REQUEST -> {
                    uploadPictureTextView.text = "Foto capturada."
                }
            }
        }
    }

    private fun registrarUsuarioEnAPI(nombre: String, usuario: String, email: String, password: String, imageUrl: String) {
        val jsonBody = JSONObject().apply {
            put("nombre", nombre)
            put("usuario", usuario)
            put("email", email)
            put("password", password)
            put("foto_perfil", imageUrl)
        }

        val requestQueue = Volley.newRequestQueue(this)
        val baseUrl = getString(R.string.base_url)
        val apiUrl = "$baseUrl/users"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, apiUrl, jsonBody,
            { response ->

                val message = response.optString("message", "Usuario registrado correctamente.")
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LogIn::class.java))
                finish()
            },
            { error ->
                val errorMessage = try {
                    val responseBody = String(error.networkResponse.data, Charsets.UTF_8)
                    val jsonError = JSONObject(responseBody)
                    jsonError.optString("message", error.message ?: "Error desconocido")
                } catch (e: Exception) {
                    error.message ?: "Error de red o servidor"
                }
                Toast.makeText(this, "Error al registrar usuario: $errorMessage", Toast.LENGTH_LONG).show()
                btnRegisterUser.isEnabled = true
            })

        requestQueue.add(jsonObjectRequest)
    }
}
