package com.example.proyectomov

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import internalStorage.SyncTransactionWorker

class MyApplication : Application() {

    private var isNetworkAvailablePreviously = false

    override fun onCreate() {
        super.onCreate()
        Log.d("MyApplication", "Application Created. Setting up network monitor.")
        setupNetworkMonitor()
    }

    private fun setupNetworkMonitor() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            private fun checkAndTriggerSync() {
                val activeNetworkInfo = connectivityManager.activeNetwork
                if (activeNetworkInfo != null) {
                    val capabilities = connectivityManager.getNetworkCapabilities(activeNetworkInfo)
                    val isInternetActuallyAvailable = capabilities != null &&
                            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))

                    if (isInternetActuallyAvailable) {
                        if (!isNetworkAvailablePreviously) {
                            Log.i("NetworkMonitor", "Network transitioned to ONLINE and VALIDATED. Enqueuing SyncTransactionWorker.")
                            enqueueSyncWorker()
                        }
                        isNetworkAvailablePreviously = true
                    } else {
                        if (isNetworkAvailablePreviously) {
                            Log.d("NetworkMonitor", "Network transitioned to OFFLINE or NOT VALIDATED.")
                        }
                        isNetworkAvailablePreviously = false
                    }
                } else {
                    if (isNetworkAvailablePreviously) {
                        Log.d("NetworkMonitor", "Network transitioned to OFFLINE (activeNetworkInfo is null).")
                    }
                    isNetworkAvailablePreviously = false
                }
            }

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d("NetworkMonitor", "Network onAvailable: $network. Checking capabilities...")
                checkAndTriggerSync()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("NetworkMonitor", "Network onLost: $network.")
                isNetworkAvailablePreviously = false
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Log.d("NetworkMonitor", "Network onCapabilitiesChanged for $network. Has Internet: ${networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}, Validated: ${networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}")
                checkAndTriggerSync()
            }
        }

        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork != null) {
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            isNetworkAvailablePreviously = caps != null &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        } else {
            isNetworkAvailablePreviously = false
        }
        Log.d("MyApplication", "Initial network available state: $isNetworkAvailablePreviously")

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        Log.d("MyApplication", "Network callback registered.")
    }

    private fun enqueueSyncWorker() {
        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncTransactionWorker>()
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "OneTimeSyncOnConnect",
            ExistingWorkPolicy.REPLACE,
            syncWorkRequest
        )
        Log.i("MyApplication", "OneTimeSyncOnConnect work request enqueued.")
    }
}