package internalStorage

import androidx.room.*
import internalStorage.GastoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GastoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGasto(gasto: GastoEntity): Long

    @Update
    suspend fun updateGasto(gasto: GastoEntity)

    @Delete
    suspend fun deleteGasto(gasto: GastoEntity)

    @Query("DELETE FROM gastos WHERE transactionId = :transactionId")
    suspend fun deleteGastoByServerId(transactionId: String)

    @Query("SELECT * FROM gastos WHERE idUser = :userId ORDER BY fecha DESC")
    fun getGastosByUser(userId: String): Flow<List<GastoEntity>>

    @Query("SELECT * FROM gastos WHERE isSynced = 0") // Get unsynced gastos
    suspend fun getUnsyncedGastos(): List<GastoEntity>

    @Query("SELECT * FROM gastos WHERE transactionId = :transactionId")
    suspend fun getGastoByServerId(transactionId: String): GastoEntity?

    @Query("SELECT * FROM gastos WHERE localId = :localId")
    suspend fun getGastoByLocalId(localId: Long): GastoEntity?

    @Query("DELETE FROM gastos WHERE localId = :localId")
    suspend fun deleteGastoByLocalId(localId: Long)

    // New methods for synchronization
    @Query("DELETE FROM gastos WHERE idUser = :userId")
    suspend fun clearAllForUser(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(gastos: List<GastoEntity>)
}