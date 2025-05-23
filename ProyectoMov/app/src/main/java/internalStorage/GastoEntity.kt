package internalStorage

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "gastos")
data class GastoEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    var transactionId: String?,
    val idUser: String,
    var nombre: String,
    var descripcion: String?,
    var fecha: String,
    var monto: Double,
    var tipo: String?,
    var archivo: String?,
    var isSynced: Boolean = true,
    var pendingAction: String? = null
)


