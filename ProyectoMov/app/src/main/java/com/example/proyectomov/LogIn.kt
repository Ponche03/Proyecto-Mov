package com.example.proyectomov

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.android.volley.Request
import org.json.JSONObject

class LogIn : AppCompatActivity() {

    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText

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

                // Llenar UsuarioGlobal
                UsuarioGlobal.id = user?.optString("_id")
                UsuarioGlobal.nombreCompleto = user?.optString("nombre")
                UsuarioGlobal.nombreUsuario = user?.optString("usuario")
                UsuarioGlobal.correo = user?.optString("email")
                UsuarioGlobal.fotoPerfil = user?.optString("foto_perfil")

                val intent = Intent(this, Dashboard::class.java)
                startActivity(intent)
            },
            { error ->
                val errorMessage = error.message ?: "Error de red o servidor"
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
            })

        val queue = Volley.newRequestQueue(this)
        queue.add(request)
    }
}
