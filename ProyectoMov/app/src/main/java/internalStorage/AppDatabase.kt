package internalStorage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import internalStorage.GastoDao
import internalStorage.IngresoDao
import internalStorage.GastoEntity
import internalStorage.IngresoEntity

@Database(entities = [GastoEntity::class, IngresoEntity::class], version = 1, exportSchema = false)
public abstract class AppDatabase : RoomDatabase() {
    abstract fun gastoDao(): GastoDao
    abstract fun ingresoDao(): IngresoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_flow_database"
                )

                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}