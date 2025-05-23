package internalStorage // Asegúrate que este sea el paquete correcto para tus clases de DB y Repositorio

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

import internalStorage.AppDatabase
import internalStorage.TransactionRepository
import internalStorage.NetworkUtils

class SyncTransactionWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    // Asegúrate que AppDatabase.getDatabase() y los DAOs son accesibles y correctos
    private val gastoDao = AppDatabase.getDatabase(applicationContext).gastoDao()
    private val ingresoDao = AppDatabase.getDatabase(applicationContext).ingresoDao()

    // Asegúrate que TransactionRepository es accesible
    private val repository by lazy { TransactionRepository(applicationContext) }


    override suspend fun doWork(): Result {
        if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
            Log.d("SyncWorker", "Network not available. Retrying later.")
            return Result.retry() // Retry if network is not available
        }

        Log.d("SyncWorker", "Starting sync process...")

        try {
            // Sync Gastos
            val unsyncedGastos = gastoDao.getUnsyncedGastos()
            Log.d("SyncWorker", "Found ${unsyncedGastos.size} unsynced gastos.")
            for (gasto in unsyncedGastos) {
                try {
                    when (gasto.pendingAction) {
                        "CREATE" -> {
                            Log.d("SyncWorker", "Syncing CREATE for gasto: ${gasto.nombre} (Local ID: ${gasto.localId})")
                            repository.registrarGasto(gasto)
                        }
                        "UPDATE" -> {
                            Log.d("SyncWorker", "Syncing UPDATE for gasto: ${gasto.nombre} (Local ID: ${gasto.localId}, Server ID: ${gasto.transactionId})")
                            if (gasto.transactionId != null) {
                                repository.actualizarGasto(gasto)
                            } else {
                                Log.e("SyncWorker", "Attempting to UPDATE gasto without transactionId: ${gasto.localId}. Treating as CREATE.")
                                repository.registrarGasto(gasto) // Treat as new if it was never synced
                            }
                        }
                        "DELETE" -> {
                            Log.d("SyncWorker", "Syncing DELETE for gasto: ${gasto.nombre} (Local ID: ${gasto.localId}, Server ID: ${gasto.transactionId})")
                            if (gasto.transactionId != null) {
                                repository.eliminarGasto(gasto)
                            } else {
                                // If it was never synced (no server ID) and marked for delete, just delete locally.
                                gastoDao.deleteGastoByLocalId(gasto.localId)
                                Log.d("SyncWorker", "Locally created gasto ${gasto.localId} deleted before sync.")
                            }
                        }
                        else -> {
                            Log.w("SyncWorker", "Unknown pendingAction '${gasto.pendingAction}' for gasto ${gasto.localId}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Error syncing individual gasto ${gasto.localId}: ${e.message}", e)
                    // Decide if this error should stop the whole worker or just skip this item
                }
            }

            // Sync Ingresos
            val unsyncedIngresos = ingresoDao.getUnsyncedIngresos()
            Log.d("SyncWorker", "Found ${unsyncedIngresos.size} unsynced ingresos.")
            for (ingreso in unsyncedIngresos) {
                try {
                    when (ingreso.pendingAction) {
                        "CREATE" -> {
                            Log.d("SyncWorker", "Syncing CREATE for ingreso: ${ingreso.nombre} (Local ID: ${ingreso.localId})")
                            repository.registrarIngreso(ingreso)
                        }
                        "UPDATE" -> {
                            Log.d("SyncWorker", "Syncing UPDATE for ingreso: ${ingreso.nombre} (Local ID: ${ingreso.localId}, Server ID: ${ingreso.transactionId})")
                            if (ingreso.transactionId != null) {
                                repository.actualizarIngreso(ingreso)
                            } else {
                                Log.e("SyncWorker", "Attempting to UPDATE ingreso without transactionId: ${ingreso.localId}. Treating as CREATE.")
                                repository.registrarIngreso(ingreso) // Treat as new if it was never synced
                            }
                        }
                        "DELETE" -> {
                            Log.d("SyncWorker", "Syncing DELETE for ingreso: ${ingreso.nombre} (Local ID: ${ingreso.localId}, Server ID: ${ingreso.transactionId})")
                            if (ingreso.transactionId != null) {
                                repository.eliminarIngreso(ingreso)
                            } else {
                                // If it was never synced (no server ID) and marked for delete, just delete locally.
                                ingresoDao.deleteIngresoByLocalId(ingreso.localId)
                                Log.d("SyncWorker", "Locally created ingreso ${ingreso.localId} deleted before sync.")
                            }
                        }
                        else -> {
                            Log.w("SyncWorker", "Unknown pendingAction '${ingreso.pendingAction}' for ingreso ${ingreso.localId}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Error syncing individual ingreso ${ingreso.localId}: ${e.message}", e)
                    // Decide if this error should stop the whole worker or just skip this item
                }
            }

            Log.d("SyncWorker", "Sync process finished successfully.")
            return Result.success()

        } catch (e: Exception) {
            Log.e("SyncWorker", "Error during overall sync process: ${e.message}", e)
            return Result.failure() // Or Result.retry() if appropriate
        }
    }
}
