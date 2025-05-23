package com.example.proyectomov

import android.content.Intent
import android.os.Bundle
import android.util.Log // Import Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.android.volley.Request
import internalStorage.TransactionRepository // Import TransactionRepository
import kotlinx.coroutines.launch // Import launch
import org.json.JSONObject

class LogIn : AppCompatActivity() {

    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText
    private val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in_usuario)

        emailEdit = findViewById(R.id.email)
        passwordEdit = findViewById(R.id.password)
        val boton = findViewById<Button>(R.id.btn_login)
        val registerLink = findViewById<TextView>(R.id.register_link)

        boton.setOnClickListener {
            val email = emailEdit.text.toString()
            val password = passwordEdit.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                logInUsuario(email, password)
            } else {
                Toast.makeText(this, "Completa todos los campos.", Toast.LENGTH_SHORT).show()
            }
        }

        registerLink.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }
    }

    private fun logInUsuario(email: String, password: String) {
        val json = JSONObject()
        json.put("email", email)
        json.put("password", password)

        val baseUrl = getString(R.string.base_url)
        val apiUrl = "$baseUrl/logIn"

        val request = JsonObjectRequest(Request.Method.POST, apiUrl, json,
            { response ->
                val message = response.optString("message")
                val user = response.optJSONObject("user")
                val token = response.optString("token")

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                UsuarioGlobal.id = user?.optString("_id")
                UsuarioGlobal.nombreCompleto = user?.optString("nombre")
                UsuarioGlobal.nombreUsuario = user?.optString("usuario")
                UsuarioGlobal.correo = user?.optString("email")
                UsuarioGlobal.fotoPerfil = user?.optString("foto_perfil")
                UsuarioGlobal.token = token

                // Start data synchronization
                UsuarioGlobal.id?.let { userId ->
                    if (userId.isNotEmpty()) {
                        lifecycleScope.launch {
                            try {
                                Log.d("LogInSync", "Attempting to sync data for user: $userId")
                                transactionRepository.synchronizeUserData(userId)
                                Log.d("LogInSync", "Synchronization process initiated for user: $userId")
                            } catch (e: Exception) {
                                Log.e("LogInSync", "Error during initial data sync: ${e.message}", e)
                                Toast.makeText(this@LogIn, "Error syncing data. Proceeding...", Toast.LENGTH_LONG).show()
                            }

                            val intent = Intent(this@LogIn, Dashboard::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Log.e("LogInSync", "User ID is empty, cannot sync.")
                        val intent = Intent(this@LogIn, Dashboard::class.java)
                        startActivity(intent)
                        finish()
                    }
                } ?: run {
                    Log.e("LogInSync", "User ID is null, cannot sync.")
                    val intent = Intent(this@LogIn, Dashboard::class.java)
                    startActivity(intent)
                    finish()
                }
            },
            { error ->
                val errorMessage = try {
                    val responseBody = String(error.networkResponse.data, Charsets.UTF_8)
                    val jsonError = JSONObject(responseBody)
                    jsonError.optString("message", error.message ?: "Error desconocido")
                } catch (e: Exception) {
                    error.message ?: "Error de red o servidor"
                }
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
            })

        val queue = Volley.newRequestQueue(this)
        queue.add(request)
    }
}