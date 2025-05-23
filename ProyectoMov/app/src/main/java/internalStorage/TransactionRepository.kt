package internalStorage

import android.content.Context
import android.util.Log
import FactoryMethod.Gasto //
import FactoryMethod.Ingreso //
import FactoryMethod.Transaccion //
import Services.TransactionService //
import internalStorage.AppDatabase
import internalStorage.GastoEntity
import internalStorage.IngresoEntity
import internalStorage.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONObject //

class TransactionRepository(private val context: Context) {
    private val transactionService = TransactionService(context) //
    private val gastoDao = AppDatabase.getDatabase(context).gastoDao()
    private val ingresoDao = AppDatabase.getDatabase(context).ingresoDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO) // A dedicated scope for repository operations

    // --- Gasto Operations ---
    suspend fun registrarGasto(gasto: GastoEntity) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {

                transactionService.registrarTransaccion(gasto.toDomain(), "gastos", //
                    onSuccess = { response ->
                        val serverId = response.optString("_id")
                        gasto.transactionId = serverId
                        gasto.isSynced = true
                        gasto.pendingAction = null
                        // If there was a local file, its URL from Firebase should be in gasto.archivo by now
                        // And gasto.localFileUri should be cleared, gasto.archivoUrlSynced = true
                        repositoryScope.launch { gastoDao.insertGasto(gasto) }
                        Log.d("Repository", "Gasto registered online and synced: ${gasto.nombre}")
                    },
                    onError = { errorMsg ->
                        Log.e("Repository", "API Error registering gasto: $errorMsg, saving locally.")
                        gasto.isSynced = false
                        gasto.pendingAction = "CREATE"
                        repositoryScope.launch { gastoDao.insertGasto(gasto) }
                    }
                )
            } catch (e: Exception) {
                Log.e("Repository", "Exception registering gasto: ${e.message}, saving locally.")
                gasto.isSynced = false
                gasto.pendingAction = "CREATE"
                repositoryScope.launch { gastoDao.insertGasto(gasto) }
            }
        } else {
            gasto.isSynced = false
            gasto.pendingAction = "CREATE"
            repositoryScope.launch { gastoDao.insertGasto(gasto) }
            Log.d("Repository", "Gasto ${gasto.nombre} registered offline, pending sync.")
        }
    }

    suspend fun actualizarGasto(gasto: GastoEntity) {
        if (NetworkUtils.isNetworkAvailable(context) && gasto.transactionId != null) {
            try {
                // Similar to registrar, handle file upload if gasto.localFileUri is present and archivoUrlSynced is false
                transactionService.actualizarTransaccion(gasto.transactionId!!, gasto.toDomain(), "gastos", //
                    onSuccess = {
                        gasto.isSynced = true
                        gasto.pendingAction = null
                        repositoryScope.launch { gastoDao.updateGasto(gasto) }
                        Log.d("Repository", "Gasto updated online and synced: ${gasto.nombre}")
                    },
                    onError = { errorMsg ->
                        Log.e("Repository", "API Error updating gasto: $errorMsg, marking for sync.")
                        gasto.isSynced = false
                        gasto.pendingAction = "UPDATE"
                        repositoryScope.launch { gastoDao.updateGasto(gasto) }
                    }
                )
            } catch (e: Exception) {
                Log.e("Repository", "Exception updating gasto: ${e.message}, marking for sync.")
                gasto.isSynced = false
                gasto.pendingAction = "UPDATE"
                repositoryScope.launch { gastoDao.updateGasto(gasto) }
            }
        } else {
            gasto.isSynced = false
            gasto.pendingAction = if (gasto.transactionId == null && gasto.pendingAction != "CREATE") "CREATE" else "UPDATE"
            // If it's a new offline item being "updated", ensure it's still marked as CREATE.
            // If it's an existing synced/unsynced item, mark as UPDATE.
            if (gasto.pendingAction == "CREATE") {
                repositoryScope.launch { gastoDao.insertGasto(gasto) } // Or update if it had a localId
            } else {
                repositoryScope.launch { gastoDao.updateGasto(gasto) }
            }
            Log.d("Repository", "Gasto update for ${gasto.nombre} saved offline, pendingAction: ${gasto.pendingAction}")
        }
    }

    suspend fun eliminarGasto(gasto: GastoEntity) {
        if (NetworkUtils.isNetworkAvailable(context) && gasto.transactionId != null) {
            try {
                transactionService.eliminarTransaccion(gasto.transactionId!!, "gastos", //
                    onSuccess = {
                        repositoryScope.launch { gastoDao.deleteGasto(gasto) }
                        Log.d("Repository", "Gasto deleted online and locally: ${gasto.nombre}")
                    },
                    onError = { errorMsg ->
                        Log.e("Repository", "API Error deleting gasto: $errorMsg, marking for sync.")
                        gasto.pendingAction = "DELETE"
                        gasto.isSynced = false
                        repositoryScope.launch { gastoDao.updateGasto(gasto) }
                    }
                )
            } catch (e: Exception) {
                Log.e("Repository", "Exception deleting gasto: ${e.message}, marking for sync.")
                gasto.pendingAction = "DELETE"
                gasto.isSynced = false
                repositoryScope.launch { gastoDao.updateGasto(gasto) }
            }
        } else {
            if (gasto.transactionId == null && gasto.pendingAction == "CREATE") { // Only created offline
                repositoryScope.launch { gastoDao.deleteGastoByLocalId(gasto.localId) }
                Log.d("Repository", "Gasto ${gasto.nombre} (created offline) deleted locally before sync.")
            } else { // Existed on server or was an update to an offline item
                gasto.pendingAction = "DELETE"
                gasto.isSynced = false
                repositoryScope.launch { gastoDao.updateGasto(gasto) }
                Log.d("Repository", "Gasto ${gasto.nombre} marked for delete offline.")
            }
        }
    }

    fun getGastos(userId: String): Flow<List<GastoEntity>> { // Removed queryParams for simplification, add back if needed
        return gastoDao.getGastosByUser(userId)
    }

    // --- Ingreso Operations ---
    suspend fun registrarIngreso(ingreso: IngresoEntity) {
        if (NetworkUtils.isNetworkAvailable(context)) {
            try {
                transactionService.registrarTransaccion(ingreso.toDomain(), "ingresos", //
                    onSuccess = { response ->
                        val serverId = response.optString("_id")
                        ingreso.transactionId = serverId
                        ingreso.isSynced = true
                        ingreso.pendingAction = null
                        repositoryScope.launch { ingresoDao.insertIngreso(ingreso) } //
                        Log.d("Repository", "Ingreso registered online and synced: ${ingreso.nombre}")
                    },
                    onError = { errorMsg ->
                        Log.e("Repository", "API Error registering ingreso: $errorMsg, saving locally.")
                        ingreso.isSynced = false
                        ingreso.pendingAction = "CREATE"
                        repositoryScope.launch { ingresoDao.insertIngreso(ingreso) } //
                    }
                )
            } catch (e: Exception) {
                Log.e("Repository", "Exception registering ingreso: ${e.message}, saving locally.")
                ingreso.isSynced = false
                ingreso.pendingAction = "CREATE"
                repositoryScope.launch { ingresoDao.insertIngreso(ingreso) } //
            }
        } else {
            ingreso.isSynced = false
            ingreso.pendingAction = "CREATE"
            repositoryScope.launch { ingresoDao.insertIngreso(ingreso) } //
            Log.d("Repository", "Ingreso ${ingreso.nombre} registered offline, pending sync.")
        }
    }

    suspend fun actualizarIngreso(ingreso: IngresoEntity) {
        if (NetworkUtils.isNetworkAvailable(context) && ingreso.transactionId != null) {
            try {
                transactionService.actualizarTransaccion(ingreso.transactionId!!, ingreso.toDomain(), "ingresos", //
                    onSuccess = {
                        ingreso.isSynced = true
                        ingreso.pendingAction = null
                        repositoryScope.launch { ingresoDao.updateIngreso(ingreso) } //
                        Log.d("Repository", "Ingreso updated online and synced: ${ingreso.nombre}")
                    },
                    onError = { errorMsg ->
                        Log.e("Repository", "API Error updating ingreso: $errorMsg, marking for sync.")
                        ingreso.isSynced = false
                        ingreso.pendingAction = "UPDATE"
                        repositoryScope.launch { ingresoDao.updateIngreso(ingreso) } //
                    }
                )
            } catch (e: Exception) {
                Log.e("Repository", "Exception updating ingreso: ${e.message}, marking for sync.")
                ingreso.isSynced = false
                ingreso.pendingAction = "UPDATE"
                repositoryScope.launch { ingresoDao.updateIngreso(ingreso) } //
            }
        } else {
            ingreso.isSynced = false
            ingreso.pendingAction = if (ingreso.transactionId == null && ingreso.pendingAction != "CREATE") "CREATE" else "UPDATE"
            if (ingreso.pendingAction == "CREATE") {
                repositoryScope.launch { ingresoDao.insertIngreso(ingreso) } //
            } else {
                repositoryScope.launch { ingresoDao.updateIngreso(ingreso) } //
            }
            Log.d("Repository", "Ingreso update for ${ingreso.nombre} saved offline, pendingAction: ${ingreso.pendingAction}")
        }
    }

    suspend fun eliminarIngreso(ingreso: IngresoEntity) {
        if (NetworkUtils.isNetworkAvailable(context) && ingreso.transactionId != null) {
            try {
                transactionService.eliminarTransaccion(ingreso.transactionId!!, "ingresos", //
                    onSuccess = {
                        repositoryScope.launch { ingresoDao.deleteIngreso(ingreso) } //
                        Log.d("Repository", "Ingreso deleted online and locally: ${ingreso.nombre}")
                    },
                    onError = { errorMsg ->
                        Log.e("Repository", "API Error deleting ingreso: $errorMsg, marking for sync.")
                        ingreso.pendingAction = "DELETE"
                        ingreso.isSynced = false
                        repositoryScope.launch { ingresoDao.updateIngreso(ingreso) } //
                    }
                )
            } catch (e: Exception) {
                Log.e("Repository", "Exception deleting ingreso: ${e.message}, marking for sync.")
                ingreso.pendingAction = "DELETE"
                ingreso.isSynced = false
                repositoryScope.launch { ingresoDao.updateIngreso(ingreso) } //
            }
        } else {
            if (ingreso.transactionId == null && ingreso.pendingAction == "CREATE") {
                repositoryScope.launch { ingresoDao.deleteIngresoByLocalId(ingreso.localId) } //
                Log.d("Repository", "Ingreso ${ingreso.nombre} (created offline) deleted locally before sync.")
            } else {
                ingreso.pendingAction = "DELETE"
                ingreso.isSynced = false
                repositoryScope.launch { ingresoDao.updateIngreso(ingreso) } //
                Log.d("Repository", "Ingreso ${ingreso.nombre} marked for delete offline.")
            }
        }
    }

    fun getIngresos(userId: String): Flow<List<IngresoEntity>> { //
        return ingresoDao.getIngresosByUser(userId) //
    }

    // --- Helper to convert Entity to Domain model for TransactionService ---
    private fun GastoEntity.toDomain(): FactoryMethod.Gasto { //
        return FactoryMethod.Gasto( //
            transactionId = this.transactionId,
            idUser = this.idUser,
            nombre = this.nombre,
            descripcion = this.descripcion,
            fecha = this.fecha,
            monto = this.monto,
            tipo = this.tipo,
            archivo = this.archivo // This should be the server URL if synced, or null/local temp if not
        )
    }

    private fun IngresoEntity.toDomain(): FactoryMethod.Ingreso { //
        return FactoryMethod.Ingreso( //
            transactionId = this.transactionId,
            idUser = this.idUser,
            nombre = this.nombre,
            descripcion = this.descripcion,
            fecha = this.fecha,
            monto = this.monto,
            tipo = this.tipo,
            archivo = this.archivo // This should be the server URL if synced, or null/local temp if not
        )
    }
}