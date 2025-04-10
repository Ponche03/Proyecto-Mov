package com.example.proyectomov

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController


class LogIn : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in_usuario) // Este layout se está mostrando

        val boton = findViewById<Button>(R.id.btn_login)

        boton.setOnClickListener {
            val intent = Intent(this, dashboard::class.java)
            startActivity(intent)
        }

    }

    override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment_content_log_in)
    return navController.navigateUp(appBarConfiguration)
            || super.onSupportNavigateUp()
    }




}