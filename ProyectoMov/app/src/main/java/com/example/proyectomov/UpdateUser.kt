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

class UpdateUser : AppCompatActivity() {

    private lateinit var fullname: EditText
    private lateinit var username: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var uploadText: TextView
    private lateinit var selectedImageUri: Uri
    private var userId: String? = null

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actualizar_usuario)

        fullname = findViewById(R.id.fullname)
        username = findViewById(R.id.username)
        passwordEdit = findViewById(R.id.password)
        uploadText = findViewById(R.id.profilepicture_text)
        val btnUpdate = findViewById<Button>(R.id.btn_login)

        userId = intent.getStringExtra("usuarioID")

        uploadText.setOnClickListener {
            mostrarOpcionesDeImagen()
        }

        btnUpdate.setOnClickListener {
            if (::selectedImageUri.isInitialized) {
                subirImagenAFirebase(selectedImageUri)
            } else {
                actualizarUsuario(null)
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
            val photoFile = crearArchivoDeImagen()
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
                    actualizarUsuario(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al subir imagen.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarUsuario(imageUrl: String?) {
        val nombre = fullname.text.toString()
        val usuario = username.text.toString()
        val password = passwordEdit.text.toString()

        val json = JSONObject()
        json.put("nombre", nombre)
        json.put("usuario", usuario)
        json.put("password", password)
        if (imageUrl != null) json.put("foto_perfil", imageUrl)

        val requestQueue = Volley.newRequestQueue(this)

        val baseUrl = getString(R.string.base_url)
        val apiUrl = "$baseUrl/users/$userId"

        val request = JsonObjectRequest(Request.Method.PUT, apiUrl, json,
            { response ->
                val message = response.optString("message")
                val usuario = response.optJSONObject("usuario")

                // Obtener datos del usuario
                val id = usuario?.optString("_id")
                val correo = usuario?.optString("Correo")
                val nombreUsuario = usuario?.optString("Nombre_Usuario")
                val nombreCompleto = usuario?.optString("Nombre_Completo")
                val fotoPerfil = usuario?.optString("Foto")

                // Mostrar mensaje de éxito
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                // Ir a otra pantalla (ejemplo: Dashboard) con los datos del usuario
                val intent = Intent(this, Dashboard::class.java)
                intent.putExtra("usuarioID", id)
                intent.putExtra("correo", correo)
                intent.putExtra("usuario", nombreUsuario)
                intent.putExtra("nombreCompleto", nombreCompleto)
                intent.putExtra("fotoPerfil", fotoPerfil)
                startActivity(intent)
                finish()
            },
            { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            })


        requestQueue.add(request)
    }
}
