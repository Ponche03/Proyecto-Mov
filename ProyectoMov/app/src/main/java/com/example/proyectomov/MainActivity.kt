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

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import internalStorage.SyncTransactionWorker //
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firebaseApp = FirebaseApp.initializeApp(this)

        if (firebaseApp != null) {
            Log.d("FirebaseInit", "Firebase initialized successfully")
        } else {
            Log.e("FirebaseInit", "Firebase failed to initialize")
        }

        setupPeriodicSync()

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

    private fun setupPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create a periodic work request
        val syncWorkRequest =
            PeriodicWorkRequestBuilder<SyncTransactionWorker>(1, TimeUnit.MINUTES)

                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "SyncTransactionWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
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