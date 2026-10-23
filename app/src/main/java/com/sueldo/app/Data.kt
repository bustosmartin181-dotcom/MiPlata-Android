package com.sueldo.app

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// ——— Enums / constantes ———
object Categorias {
    val GASTO = listOf("Comida", "Transporte", "Super", "Salidas", "Salud", "Otro")
    val INGRESO = listOf("Sueldo", "Freelance", "Alquiler", "Otro ingreso")
}

// ——— Entidades Room ———
@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val nombre: String = "",
    val sueldo: Double = 1_847_755.62,
    val saldoPrevio: Double = 26_255.87,
    val fechaInicio: String = "2026-10-06",
    val fechaFin: String = "2026-11-04",
    val ajusteManualDiario: Double? = null,
    val objetivoPorDia: Double? = null,
    val pin: String = "",
    val pinAlAbrir: Boolean = false,
    val pinSoloMontos: Boolean = false,
    val tema: String = "auto", // auto | claro | oscuro
    val colorAccent: Long = 0xFF0F766E,
    val ocultarMontos: Boolean = false,
    val avisoPorcentaje: Int = 80,
    val recordatorioHora: String = "20:00",
    val recordatorioActivo: Boolean = false,
    val backupCadaDias: Int = 7,
    val ultimoBackup: String = "",
    val onboardingHecho: Boolean = false,
    val demoMode: Boolean = false
)

@Entity(tableName = "gasto")
data class Gasto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: String,
    val monto: Double,
    val detalle: String = "",
    val categoria: String = "Otro",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "gasto_importante")
data class GastoImportante(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val concepto: String,
    val monto: Double,
    val pagado: Boolean = false,
    val repetirMesSiguiente: Boolean = false,
    val montoVariable: Boolean = false,
    val enCuotas: Boolean = false,
    val cuotaActual: Int = 0,
    val cuotaTotal: Int = 0,
    val orden: Int = 0
)

@Entity(tableName = "ingreso")
data class Ingreso(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val concepto: String,
    val monto: Double,
    val categoria: String = "Sueldo",
    val orden: Int = 0
)

@Entity(tableName = "cuenta")
data class Cuenta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val saldo: Double = 0.0,
    val orden: Int = 0
)

@Entity(tableName = "meta")
data class Meta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val montoObjetivo: Double,
    val montoJuntado: Double = 0.0,
    val fechaObjetivo: String = "",
    val activa: Boolean = true
)

@Entity(tableName = "dia_estado")
data class DiaEstado(
    @PrimaryKey val fecha: String,
    val cerrado: Boolean = false,
    val nota: String = ""
)

@Entity(tableName = "periodo_hist")
data class PeriodoHist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fechaInicio: String,
    val fechaFin: String,
    val resumenJson: String = "",
    val cerradoEn: Long = System.currentTimeMillis()
)

// ——— UI models ———
data class ResumenHoy(
    val nombre: String,
    val presupuestoHoy: Double,
    val gastadoHoy: Double,
    val sobranteHoy: Double,
    val mananaEstimado: Double,
    val semaforo: Semaforo,
    val fraseEstado: String,
    val fraseDelDia: String,
    val diasRestantes: Int,
    val basePorDia: Double,
    val baseCambiaManana: Boolean,
    val totalPeriodoDisponible: Double,
    val quedaPeriodo: Double,
    val racha: List<Boolean>,
    val ocultarMontos: Boolean
)

enum class Semaforo { VERDE, AMARILLO, ROJO }

data class DiaUi(
    val fecha: LocalDate,
    val diaSemana: String,
    val presupuesto: Double,
    val gastoReal: Double,
    val sobrante: Double,
    val cerrado: Boolean,
    val esHoy: Boolean,
    val gastos: List<Gasto>
)

// ——— DAOs ———
@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun get(): Flow<Settings?>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(s: Settings)
}

@Dao
interface GastoDao {
    @Query("SELECT * FROM gasto ORDER BY fecha DESC, createdAt DESC")
    fun all(): Flow<List<Gasto>>
    @Query("SELECT * FROM gasto WHERE fecha = :fecha ORDER BY createdAt DESC")
    fun byFecha(fecha: String): Flow<List<Gasto>>
    @Insert suspend fun insert(g: Gasto): Long
    @Update suspend fun update(g: Gasto)
    @Query("DELETE FROM gasto WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface GastoImportanteDao {
    @Query("SELECT * FROM gasto_importante ORDER BY orden ASC")
    fun all(): Flow<List<GastoImportante>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(g: GastoImportante): Long
    @Update suspend fun update(g: GastoImportante)
    @Query("DELETE FROM gasto_importante WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT COUNT(*) FROM gasto_importante") suspend fun count(): Int
}

@Dao
interface IngresoDao {
    @Query("SELECT * FROM ingreso ORDER BY orden ASC")
    fun all(): Flow<List<Ingreso>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(i: Ingreso): Long
    @Update suspend fun update(i: Ingreso)
    @Query("DELETE FROM ingreso WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface CuentaDao {
    @Query("SELECT * FROM cuenta ORDER BY orden ASC")
    fun all(): Flow<List<Cuenta>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(c: Cuenta): Long
    @Update suspend fun update(c: Cuenta)
    @Query("DELETE FROM cuenta WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface MetaDao {
    @Query("SELECT * FROM meta WHERE activa = 1 ORDER BY id DESC")
    fun activas(): Flow<List<Meta>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(m: Meta): Long
    @Update suspend fun update(m: Meta)
    @Query("DELETE FROM meta WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface DiaEstadoDao {
    @Query("SELECT * FROM dia_estado")
    fun all(): Flow<List<DiaEstado>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(d: DiaEstado)
}

@Dao
interface PeriodoHistDao {
    @Query("SELECT * FROM periodo_hist ORDER BY cerradoEn DESC")
    fun all(): Flow<List<PeriodoHist>>
    @Insert suspend fun insert(p: PeriodoHist): Long
}

@Database(
    entities = [
        Settings::class, Gasto::class, GastoImportante::class, Ingreso::class,
        Cuenta::class, Meta::class, DiaEstado::class, PeriodoHist::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settings(): SettingsDao
    abstract fun gasto(): GastoDao
    abstract fun gastoImportante(): GastoImportanteDao
    abstract fun ingreso(): IngresoDao
    abstract fun cuenta(): CuentaDao
    abstract fun meta(): MetaDao
    abstract fun diaEstado(): DiaEstadoDao
    abstract fun periodoHist(): PeriodoHistDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(ctx: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(ctx, AppDatabase::class.java, "mi_plata.db")
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}

// ——— Repository ———
class Repository(private val db: AppDatabase) {
    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val locale = Locale("es", "AR")

    val settings = db.settings().get().map { it ?: Settings() }
    val gastos = db.gasto().all()
    val gastosImportantes = db.gastoImportante().all()
    val ingresos = db.ingreso().all()
    val cuentas = db.cuenta().all()
    val metas = db.meta().activas()
    val diasEstado = db.diaEstado().all()

    suspend fun saveSettings(s: Settings) = db.settings().save(s)
    suspend fun addGasto(g: Gasto) = db.gasto().insert(g)
    suspend fun updateGasto(g: Gasto) = db.gasto().update(g)
    suspend fun deleteGasto(id: Long) = db.gasto().delete(id)
    suspend fun updateImportante(g: GastoImportante) = db.gastoImportante().update(g)
    suspend fun addImportante(g: GastoImportante) = db.gastoImportante().insert(g)
    suspend fun deleteImportante(id: Long) = db.gastoImportante().delete(id)
    suspend fun addIngreso(i: Ingreso) = db.ingreso().insert(i)
    suspend fun updateIngreso(i: Ingreso) = db.ingreso().update(i)
    suspend fun deleteIngreso(id: Long) = db.ingreso().delete(id)
    suspend fun addCuenta(c: Cuenta) = db.cuenta().insert(c)
    suspend fun updateCuenta(c: Cuenta) = db.cuenta().update(c)
    suspend fun deleteCuenta(id: Long) = db.cuenta().delete(id)
    suspend fun addMeta(m: Meta) = db.meta().insert(m)
    suspend fun updateMeta(m: Meta) = db.meta().update(m)
    suspend fun deleteMeta(id: Long) = db.meta().delete(id)
    suspend fun setDiaCerrado(fecha: String, cerrado: Boolean) =
        db.diaEstado().upsert(DiaEstado(fecha, cerrado))

    suspend fun seedIfNeeded() {
        if (db.gastoImportante().count() == 0) {
            listOf(
                GastoImportante(concepto = "Telecom personal", monto = 57172.0, pagado = true, repetirMesSiguiente = true, orden = 0),
                GastoImportante(concepto = "Préstamo Maríela", monto = 260000.0, pagado = true, orden = 1),
                GastoImportante(concepto = "Alejandro Bustos", monto = 138310.0, pagado = true, orden = 2),
                GastoImportante(concepto = "Musimundo", monto = 77302.0, pagado = true, enCuotas = true, cuotaActual = 3, cuotaTotal = 12, orden = 3),
                GastoImportante(concepto = "Seguro moto", monto = 30616.0, pagado = true, repetirMesSiguiente = true, orden = 4),
                GastoImportante(concepto = "Wiber internet", monto = 53905.0, pagado = true, repetirMesSiguiente = true, orden = 5),
                GastoImportante(concepto = "Reserva supermercado", monto = 200000.0, pagado = true, orden = 6),
                GastoImportante(concepto = "Debo a Orlando", monto = 50000.0, pagado = true, orden = 7),
                GastoImportante(concepto = "Reserva asados trabajo", monto = 60000.0, pagado = true, orden = 8),
                GastoImportante(concepto = "Tarjeta Naranja", monto = 217109.0, pagado = true, orden = 9),
                GastoImportante(concepto = "Nafta", monto = 90412.0, pagado = true, orden = 10),
                GastoImportante(concepto = "Posible desagote pluvial", monto = 40000.0, pagado = true, montoVariable = true, orden = 11)
            ).forEach { db.gastoImportante().insert(it) }
            db.settings().save(Settings())
            db.ingreso().insert(Ingreso(concepto = "Sueldo", monto = 1_847_755.62, categoria = "Sueldo"))
            db.cuenta().insert(Cuenta(nombre = "Cuenta sueldo", saldo = 26_255.87))
            db.cuenta().insert(Cuenta(nombre = "Efectivo", saldo = 0.0, orden = 1))
        }
    }

    fun resumenHoyFlow(): Flow<ResumenHoy> = combine(settings, gastos, gastosImportantes, diasEstado) { s, gs, imp, _ ->
        calcularResumenHoy(s, gs, imp)
    }

    fun diasUiFlow(): Flow<List<DiaUi>> = combine(settings, gastos, gastosImportantes, diasEstado) { s, gs, imp, de ->
        calcularDias(s, gs, imp, de)
    }

    private fun calcularBase(s: Settings, imp: List<GastoImportante>): Pair<Double, Int> {
        val totalImp = imp.sumOf { it.monto }
        val disponible = s.sueldo + s.saldoPrevio - totalImp
        val inicio = LocalDate.parse(s.fechaInicio)
        val fin = LocalDate.parse(s.fechaFin)
        val dias = (ChronoUnit.DAYS.between(inicio, fin) + 1).toInt().coerceAtLeast(1)
        val base = s.ajusteManualDiario ?: s.objetivoPorDia ?: (disponible / dias)
        return base to dias
    }

    private fun calcularResumenHoy(s: Settings, gs: List<Gasto>, imp: List<GastoImportante>): ResumenHoy {
        val (base, diasTotales) = calcularBase(s, imp)
        val inicio = LocalDate.parse(s.fechaInicio)
        val fin = LocalDate.parse(s.fechaFin)
        val hoy = LocalDate.now()
        val gastosPorFecha = gs.groupBy { it.fecha }

        var carry = 0.0
        var current = inicio
        var presupuestoHoy = base
        var gastadoHoy = 0.0
        val racha = mutableListOf<Boolean>()

        while (!current.isAfter(fin)) {
            val key = current.format(fmt)
            val gastado = gastosPorFecha[key]?.sumOf { it.monto } ?: 0.0
            val presup = base + carry
            val sob = presup - gastado
            if (current == hoy) {
                presupuestoHoy = presup
                gastadoHoy = gastado
            }
            if (!current.isAfter(hoy) && gastosPorFecha.containsKey(key)) {
                racha.add(sob >= 0)
            }
            carry = if (gastosPorFecha.containsKey(key) || current.isBefore(hoy)) sob else 0.0
            // carry only from days that have data or past
            if (!gastosPorFecha.containsKey(key) && !current.isBefore(hoy)) carry = 0.0
            // simpler: always pass sobrante if we have gasto registered OR day is past with 0
            current = current.plusDays(1)
        }

        // Recalcular carry correctamente
        carry = 0.0
        current = inicio
        while (current.isBefore(hoy)) {
            val key = current.format(fmt)
            val gastado = gastosPorFecha[key]?.sumOf { it.monto } ?: 0.0
            val presup = base + carry
            carry = presup - gastado
            current = current.plusDays(1)
        }
        presupuestoHoy = base + carry
        gastadoHoy = gastosPorFecha[hoy.format(fmt)]?.sumOf { it.monto } ?: 0.0
        val sobranteHoy = presupuestoHoy - gastadoHoy
        val manana = base + sobranteHoy

        val ratio = if (presupuestoHoy > 0) gastadoHoy / presupuestoHoy else 0.0
        val semaforo = when {
            ratio <= 0.7 -> Semaforo.VERDE
            ratio <= 1.0 -> Semaforo.AMARILLO
            else -> Semaforo.ROJO
        }
        val fraseEstado = when (semaforo) {
            Semaforo.VERDE -> "Vas muy bien hoy"
            Semaforo.AMARILLO -> "Cuidado, ya usaste buena parte del día"
            Semaforo.ROJO -> "Te pasaste del presupuesto de hoy"
        }
        val frases = listOf(
            "Cada peso cuenta.",
            "Hoy es un buen día para elegir.",
            "Tu plata, tu decisión.",
            "Pequeños gastos, grandes resultados."
        )
        val fraseDelDia = frases[(hoy.dayOfYear) % frases.size]

        val diasRestantes = ChronoUnit.DAYS.between(hoy, fin).toInt().coerceAtLeast(0)
        val totalImp = imp.sumOf { it.monto }
        val disponible = s.sueldo + s.saldoPrevio - totalImp
        val gastadoPeriodo = gs.sumOf { it.monto }

        return ResumenHoy(
            nombre = s.nombre.ifBlank { "vos" },
            presupuestoHoy = presupuestoHoy,
            gastadoHoy = gastadoHoy,
            sobranteHoy = sobranteHoy,
            mananaEstimado = manana,
            semaforo = semaforo,
            fraseEstado = fraseEstado,
            fraseDelDia = fraseDelDia,
            diasRestantes = diasRestantes,
            basePorDia = base,
            baseCambiaManana = s.ajusteManualDiario != null || s.objetivoPorDia != null,
            totalPeriodoDisponible = disponible,
            quedaPeriodo = disponible - gastadoPeriodo,
            racha = racha.takeLast(14),
            ocultarMontos = s.ocultarMontos
        )
    }

    private fun calcularDias(
        s: Settings,
        gs: List<Gasto>,
        imp: List<GastoImportante>,
        de: List<DiaEstado>
    ): List<DiaUi> {
        val (base, _) = calcularBase(s, imp)
        val inicio = LocalDate.parse(s.fechaInicio)
        val fin = LocalDate.parse(s.fechaFin)
        val hoy = LocalDate.now()
        val mapaG = gs.groupBy { it.fecha }
        val mapaC = de.associateBy { it.fecha }
        val lista = mutableListOf<DiaUi>()
        var carry = 0.0
        var current = inicio
        while (!current.isAfter(fin)) {
            val key = current.format(fmt)
            val listaG = mapaG[key] ?: emptyList()
            val gastado = listaG.sumOf { it.monto }
            val presup = base + carry
            val sob = presup - gastado
            lista.add(
                DiaUi(
                    fecha = current,
                    diaSemana = current.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                        .replaceFirstChar { it.uppercase() },
                    presupuesto = presup,
                    gastoReal = gastado,
                    sobrante = sob,
                    cerrado = mapaC[key]?.cerrado == true || current.isBefore(hoy),
                    esHoy = current == hoy,
                    gastos = listaG
                )
            )
            // carry if day has expenses or is past
            if (listaG.isNotEmpty() || current.isBefore(hoy)) carry = sob else carry = 0.0
            current = current.plusDays(1)
        }
        return lista
    }

    suspend fun recalibrar() {
        val s = db.settings().get().map { it ?: Settings() }
        // simplified: clear manual adjust so base recalculates from remaining
        // real logic applied in UI via settings update
    }

    suspend fun nuevoPeriodo() {
        val s = (db.settings().get().map { it ?: Settings() }).let {
            // get current once - use blocking-style via existing
            Settings()
        }
        // Keep recurring importantes; archive handled in ViewModel
    }

    fun exportCsvDias(dias: List<DiaUi>): String {
        val sb = StringBuilder("fecha,dia,presupuesto,gasto,sobrante\n")
        dias.forEach {
            sb.append("${it.fecha},${it.diaSemana},${it.presupuesto},${it.gastoReal},${it.sobrante}\n")
        }
        return sb.toString()
    }
}
