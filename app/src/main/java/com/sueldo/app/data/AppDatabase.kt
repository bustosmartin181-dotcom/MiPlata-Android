package com.sueldo.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    @Query("SELECT * FROM config WHERE id = 1")
    fun getConfig(): Flow<Config?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(config: Config)
}

@Dao
interface GastoImportanteDao {
    @Query("SELECT * FROM gasto_importante ORDER BY orden ASC")
    fun getAll(): Flow<List<GastoImportante>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(gasto: GastoImportante): Long

    @Update
    suspend fun update(gasto: GastoImportante)

    @Query("DELETE FROM gasto_importante WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM gasto_importante")
    suspend fun count(): Int
}

@Dao
interface DiaDao {
    @Query("SELECT * FROM dia")
    fun getAll(): Flow<List<Dia>>

    @Query("SELECT * FROM dia WHERE fecha = :fecha")
    suspend fun getByFecha(fecha: String): Dia?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dia: Dia)
}

@Database(
    entities = [Config::class, GastoImportante::class, Dia::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun gastoImportanteDao(): GastoImportanteDao
    abstract fun diaDao(): DiaDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sueldo_control.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
