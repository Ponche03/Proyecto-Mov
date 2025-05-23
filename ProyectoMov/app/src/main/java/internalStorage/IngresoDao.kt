package internalStorage

import androidx.room.*
import internalStorage.IngresoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngresoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngreso(ingreso: IngresoEntity): Long

    @Update
    suspend fun updateIngreso(ingreso: IngresoEntity)

    @Delete
    suspend fun deleteIngreso(ingreso: IngresoEntity)

    @Query("DELETE FROM ingresos WHERE transactionId = :transactionId")
    suspend fun deleteIngresoByServerId(transactionId: String)

    @Query("SELECT * FROM ingresos WHERE idUser = :userId ORDER BY fecha DESC")
    fun getIngresosByUser(userId: String): Flow<List<IngresoEntity>>

    @Query("SELECT * FROM ingresos WHERE isSynced = 0")
    suspend fun getUnsyncedIngresos(): List<IngresoEntity>

    @Query("SELECT * FROM ingresos WHERE transactionId = :transactionId")
    suspend fun getIngresoByServerId(transactionId: String): IngresoEntity?

    @Query("SELECT * FROM ingresos WHERE localId = :localId")
    suspend fun getIngresoByLocalId(localId: Long): IngresoEntity?

    @Query("DELETE FROM ingresos WHERE localId = :localId")
    suspend fun deleteIngresoByLocalId(localId: Long)

    // New methods for synchronization
    @Query("DELETE FROM ingresos WHERE idUser = :userId")
    suspend fun clearAllForUser(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingresos: List<IngresoEntity>)
}