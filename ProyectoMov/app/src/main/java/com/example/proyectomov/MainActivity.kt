package com.example.proyectomov

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.proyectomov.ui.theme.ProyectoMovTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firebaseApp = FirebaseApp.initializeApp(this)

        if (firebaseApp != null) {
            Log.d("FirebaseInit", "Firebase initialized successfully")
        } else {
            Log.e("FirebaseInit", "Firebase failed to initialize")
        }

        val intent = Intent(this, LogIn::class.java)
        startActivity(intent)

        //enableEdgeToEdge()
       // setContent {
       //     ProyectoMovTheme {
       //         Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
       //             Greeting(
       //                 name = "Android",
       //                 modifier = Modifier.padding(innerPadding)
       //             )
       //         }
       //     }
       // }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ProyectoMovTheme {
        Greeting("Android")
    }
}