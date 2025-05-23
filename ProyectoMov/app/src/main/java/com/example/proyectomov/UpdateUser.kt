package com.example.proyectomov

import UsuarioGlobal
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.android.volley.Response
import Services.FirebaseStorageService
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class UpdateUser : AppCompatActivity() {

    private lateinit var fullnameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var uploadPictureTextView: TextView
    private lateinit var btnUpdateUser: Button


    private lateinit var botonRegresar: ImageView

    private var selectedImageUri: Uri? = null
    private var userId: String? = null

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private var currentPhotoPath: String? = null

    private val firebaseStorageService = FirebaseStorageService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_actualizar_usuario)


        fullnameEditText = findViewById(R.id.fullname)
        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        uploadPictureTextView = findViewById(R.id.profilepicture_text)
        btnUpdateUser = findViewById(R.id.btn_login)
        botonRegresar = findViewById(R.id.imageView4)

        userId = UsuarioGlobal.id

        fullnameEditText.setText(UsuarioGlobal.nombreCompleto)
        usernameEditText.setText(UsuarioGlobal.nombreUsuario)

        uploadPictureTextView.setOnClickListener {
            mostrarOpcionesDeImagen()
        }

        btnUpdateUser.setOnClickListener {
            handleUserUpdate()
        }

        botonRegresar.setOnClickListener {
            startActivity(Intent(this, Dashboard::class.java))
            finish()
        }
    }

    private fun handleUserUpdate() {
        val nombre = fullnameEditText.text.toString().trim()
        val usuario = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (nombre.isEmpty() || usuario.isEmpty()) {
            Toast.makeText(this, "Nombre completo y nombre de usuario no pueden estar vacíos.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Actualizando usuario...", Toast.LENGTH_SHORT).show()
        btnUpdateUser.isEnabled = false

        if (selectedImageUri != null) {
            firebaseStorageService.uploadFile(
                fileUri = selectedImageUri!!,
                storagePath = "perfil_imagenes",
                onSuccess = { newImageUrl ->
                    actualizarUsuarioAPI(nombre, usuario, password, newImageUrl)
                },
                onFailure = { errorMessage ->
                    Toast.makeText(this, "Error al subir nueva imagen: $errorMessage", Toast.LENGTH_LONG).show()
                    btnUpdateUser.isEnabled = true
                }
            )
        } else {
            actualizarUsuarioAPI(nombre, usuario, password, null)
        }
    }

    private fun mostrarOpcionesDeImagen() {
        val opciones = arrayOf("Seleccionar desde Galería", "Tomar Foto")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cambiar foto de perfil")
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
                        uploadPictureTextView.text = "Nueva imagen: ${it.lastPathSegment ?: "seleccionada"}"
                    }
                }
                TAKE_PHOTO_REQUEST -> {
                    uploadPictureTextView.text = "Nueva foto capturada."
                }
            }
        }
    }

    private fun actualizarUsuarioAPI(nombreCompleto: String, nombreUsuario: String, passwordInput: String, newImageUrl: String?) {
        if (userId == null) {
            Toast.makeText(this, "Error: ID de usuario no disponible.", Toast.LENGTH_LONG).show()
            btnUpdateUser.isEnabled = true
            return
        }

        val jsonBody = JSONObject().apply {
            put("Nombre_Completo", nombreCompleto)
            put("Nombre_Usuario", nombreUsuario)

            if (passwordInput.isNotBlank()) {
                put("password", passwordInput)
            }
            newImageUrl?.let {
                put("Foto", it)
            }
        }

        val requestQueue = Volley.newRequestQueue(this)
        val baseUrl = getString(R.string.base_url)
        val apiUrl = "$baseUrl/users/$userId"

        val jsonObjectRequestWithAuth = object : JsonObjectRequest(
            Method.PUT,
            apiUrl,
            jsonBody,
            Response.Listener { response ->
                val message = response.optString("message", "Usuario actualizado correctamente.")
                val updatedUser = response.optJSONObject("usuario")

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                updatedUser?.let {
                    UsuarioGlobal.id = it.optString("_id", UsuarioGlobal.id)
                    UsuarioGlobal.nombreCompleto = it.optString("Nombre_Completo", UsuarioGlobal.nombreCompleto)
                    UsuarioGlobal.nombreUsuario = it.optString("Nombre_Usuario", UsuarioGlobal.nombreUsuario)
                    UsuarioGlobal.correo = it.optString("email", UsuarioGlobal.correo) // Asumiendo que el backend devuelve 'email'
                    UsuarioGlobal.fotoPerfil = it.optString("Foto", UsuarioGlobal.fotoPerfil)
                }

                startActivity(Intent(this, Dashboard::class.java))
                finish()
            },
            Response.ErrorListener { error ->
                val errorMessage = try {
                    val responseBody = String(error.networkResponse.data, Charsets.UTF_8)
                    val jsonError = JSONObject(responseBody)
                    jsonError.optString("message", error.message ?: "Error desconocido") // Tu backend devuelve "error" en caso de 500 o 404
                } catch (e: Exception) {
                    error.message ?: "Error de red o servidor"
                }
                Toast.makeText(this, "Error al actualizar usuario: $errorMessage", Toast.LENGTH_LONG).show()
                btnUpdateUser.isEnabled = true
            }
        ) {
            @Throws(com.android.volley.AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                val token = UsuarioGlobal.token
                if (!token.isNullOrEmpty()) {
                    headers["Authorization"] = "Bearer $token"
                }
                return headers
            }
        }
        requestQueue.add(jsonObjectRequestWithAuth)
    }
}
