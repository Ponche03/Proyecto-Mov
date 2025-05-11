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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.android.volley.Request
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

class Register : AppCompatActivity() {

    private lateinit var fullname: EditText
    private lateinit var username: EditText
    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var uploadText: TextView
    private lateinit var selectedImageUri: Uri

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro_usuario)

        fullname = findViewById(R.id.fullname)
        username = findViewById(R.id.username)
        emailEdit = findViewById(R.id.email)
        passwordEdit = findViewById(R.id.password)
        uploadText = findViewById(R.id.profilepicture_text)
        val btnRegister = findViewById<Button>(R.id.btn_login)
        val logInLink = findViewById<TextView>(R.id.register_link)

        logInLink.setOnClickListener {
            startActivity(Intent(this, LogIn::class.java))
        }

        // Subir imagen: Mostrar opciones
        uploadText.setOnClickListener {
            mostrarOpcionesDeImagen()
        }

        // Registrar
        btnRegister.setOnClickListener {
            if (::selectedImageUri.isInitialized) {
                subirImagenAFirebase(selectedImageUri)
            } else {
                Toast.makeText(this, "Selecciona una imagen de perfil", Toast.LENGTH_SHORT).show()
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
        val downloadsUri = Uri.parse("content://downloads/public_downloads")
        intent.data = downloadsUri
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
            File.createTempFile(nombreImagen, ".jpg", storageDir)
        } catch (e: IOException) {
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    selectedImageUri = data?.data!!
                    uploadText.text = "Imagen seleccionada."
                }
                TAKE_PHOTO_REQUEST -> {
                    uploadText.text = "Foto tomada."
                }
            }
        }
    }

    private fun subirImagenAFirebase(uri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference.child("perfil_imagenes/${UUID.randomUUID()}")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    registrarUsuarioEnAPI(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al subir imagen", Toast.LENGTH_SHORT).show()
            }
    }

    private fun registrarUsuarioEnAPI(imageUrl: String) {
        val nombre = fullname.text.toString()
        val usuario = username.text.toString()
        val email = emailEdit.text.toString()
        val password = passwordEdit.text.toString()

        val json = JSONObject()
        json.put("nombre", nombre)
        json.put("usuario", usuario)
        json.put("email", email)
        json.put("password", password)
        json.put("foto_perfil", imageUrl)

        val requestQueue = Volley.newRequestQueue(this)

        val baseUrl = getString(R.string.base_url)
        val apiUrl = "$baseUrl/users"

        val request = JsonObjectRequest(Request.Method.POST, apiUrl, json,
            { response ->
                Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LogIn::class.java))
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            })

        requestQueue.add(request)
    }
}
