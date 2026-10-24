package com.sueldo.app

import android.content.Context
import java.security.MessageDigest

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

object PinHash {
    fun hash(pin: String): String {
        if (pin.isBlank()) return ""
        val dig = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
        return dig.joinToString("") { "%02x".format(it) }
    }
    /** Acepta PIN en claro (legacy) o hash SHA-256. */
    fun matches(stored: String, input: String): Boolean {
        if (stored.isBlank()) return true
        if (stored == input) return true // legacy texto plano
        return stored == hash(input)
    }
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
    @Query("DELETE FROM gasto")
    suspend fun deleteAll()
    @Query("SELECT * FROM gasto ORDER BY fecha DESC")
    suspend fun listAll(): List<Gasto>
}

@Dao
interface GastoImportanteDao {
    @Query("SELECT * FROM gasto_importante ORDER BY orden ASC")
    fun all(): Flow<List<GastoImportante>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(g: GastoImportante): Long
    @Update suspend fun update(g: GastoImportante)
    @Query("DELETE FROM gasto_importante WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT COUNT(*) FROM gasto_importante") suspend fun count(): Int
    @Query("DELETE FROM gasto_importante") suspend fun deleteAll()
    @Query("SELECT * FROM gasto_importante ORDER BY orden ASC")
    suspend fun listAll(): List<GastoImportante>
}

@Dao
interface IngresoDao {
    @Query("SELECT * FROM ingreso ORDER BY orden ASC")
    fun all(): Flow<List<Ingreso>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(i: Ingreso): Long
    @Update suspend fun update(i: Ingreso)
    @Query("DELETE FROM ingreso WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM ingreso") suspend fun deleteAll()
    @Query("SELECT * FROM ingreso ORDER BY orden ASC")
    suspend fun listAll(): List<Ingreso>
}

@Dao
interface CuentaDao {
    @Query("SELECT * FROM cuenta ORDER BY orden ASC")
    fun all(): Flow<List<Cuenta>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(c: Cuenta): Long
    @Update suspend fun update(c: Cuenta)
    @Query("DELETE FROM cuenta WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM cuenta") suspend fun deleteAll()
    @Query("SELECT * FROM cuenta ORDER BY orden ASC")
    suspend fun listAll(): List<Cuenta>
}

@Dao
interface MetaDao {
    @Query("SELECT * FROM meta WHERE activa = 1 ORDER BY id DESC")
    fun activas(): Flow<List<Meta>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(m: Meta): Long
    @Update suspend fun update(m: Meta)
    @Query("DELETE FROM meta WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM meta") suspend fun deleteAll()
    @Query("SELECT * FROM meta ORDER BY id ASC")
    suspend fun listAll(): List<Meta>
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


    private fun totalIngresos(s: Settings, ingresos: List<Ingreso>): Double {
        val suma = ingresos.sumOf { it.monto }
        return if (suma > 0) suma else s.sueldo
    }

    private fun calcularBase(s: Settings, imp: List<GastoImportante>, ingresos: List<Ingreso>): Pair<Double, Int> {
        val totalImp = imp.sumOf { it.monto }
        val disponible = totalIngresos(s, ingresos) + s.saldoPrevio - totalImp
        val inicio = LocalDate.parse(s.fechaInicio)
        val fin = LocalDate.parse(s.fechaFin)
        val dias = (ChronoUnit.DAYS.between(inicio, fin) + 1).toInt().coerceAtLeast(1)
        val base = s.ajusteManualDiario ?: s.objetivoPorDia ?: (disponible / dias)
        return base to dias
    }

    /** Simula el periodo día a día con traspaso siempre (como la app original). */
    private fun simularDias(
        s: Settings,
        gs: List<Gasto>,
        imp: List<GastoImportante>,
        ingresos: List<Ingreso>,
        de: List<DiaEstado> = emptyList()
    ): List<DiaUi> {
        val (base, _) = calcularBase(s, imp, ingresos)
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
            // Los negativos del JSON son reintegros: suman al "gastado" neto
            val gastado = listaG.sumOf { it.monto }
            val presup = base + carry
            val sob = presup - gastado
            val cerrado = mapaC[key]?.cerrado == true || current.isBefore(hoy)
            lista.add(
                DiaUi(
                    fecha = current,
                    diaSemana = current.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                        .replaceFirstChar { it.uppercase() },
                    presupuesto = presup,
                    gastoReal = gastado,
                    sobrante = sob,
                    cerrado = cerrado,
                    esHoy = current == hoy,
                    gastos = listaG
                )
            )
            // Traspaso siempre al día siguiente (días sin gasto = gastado 0)
            carry = sob
            current = current.plusDays(1)
        }
        return lista
    }

    fun resumenHoyFlow(): Flow<ResumenHoy> =
        combine(settings, gastos, gastosImportantes, ingresos) { s, gs, imp, ing ->
            calcularResumenHoy(s, gs, imp, ing)
        }

    fun diasUiFlow(): Flow<List<DiaUi>> =
        combine(settings, gastos, gastosImportantes, ingresos, diasEstado) { s, gs, imp, ing, de ->
            simularDias(s, gs, imp, ing, de)
        }

    private fun calcularResumenHoy(
        s: Settings,
        gs: List<Gasto>,
        imp: List<GastoImportante>,
        ingresos: List<Ingreso>
    ): ResumenHoy {
        val (base, diasTotales) = calcularBase(s, imp, ingresos)
        val inicio = LocalDate.parse(s.fechaInicio)
        val fin = LocalDate.parse(s.fechaFin)
        val hoy = LocalDate.now()
        val dias = simularDias(s, gs, imp, ingresos)
        val diaHoy = dias.find { it.esHoy }
        val presupuestoHoy = diaHoy?.presupuesto ?: base
        val gastadoHoy = diaHoy?.gastoReal ?: 0.0
        val sobranteHoy = diaHoy?.sobrante ?: presupuestoHoy
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
        val disponible = totalIngresos(s, ingresos) + s.saldoPrevio - totalImp
        val gastadoPeriodo = gs.sumOf { it.monto }
        val racha = dias.filter { !it.fecha.isAfter(hoy) }.takeLast(14).map { it.sobrante >= 0 }

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
            racha = racha,
            ocultarMontos = s.ocultarMontos
        )
    }


    suspend fun exportBackupJson(): String {
        val s = db.settings().get() ?: Settings()
        val root = org.json.JSONObject()
        root.put("sueldo", s.sueldo)
        root.put("previo", s.saldoPrevio)
        root.put("start", s.fechaInicio)
        root.put("end", s.fechaFin)
        root.put("nombre", s.nombre)
        root.put("tema", s.tema)
        root.put("bienvenida", true)
        s.objetivoPorDia?.let { root.put("objetivo", it) }
        root.put("upd", System.currentTimeMillis())
        root.put("ultimaCopia", System.currentTimeMillis())

        val items = org.json.JSONArray()
        db.gastoImportante().listAll().forEach { g ->
            items.put(org.json.JSONObject().apply {
                put("n", g.concepto)
                put("a", g.monto)
                put("p", g.pagado)
                put("v", g.montoVariable)
                put("f", g.repetirMesSiguiente)
            })
        }
        root.put("items", items)

        val spent = org.json.JSONArray()
        db.gasto().listAll().forEach { g ->
            spent.put(org.json.JSONObject().apply {
                put("id", g.id)
                put("date", g.fecha)
                put("amt", g.monto)
                put("note", g.detalle)
                put("cat", g.categoria)
            })
        }
        root.put("spent", spent)

        val ingresos = org.json.JSONArray()
        db.ingreso().listAll().forEach { i ->
            ingresos.put(org.json.JSONObject().apply {
                put("cat", i.concepto)
                put("a", i.monto)
            })
        }
        root.put("ingresos", ingresos)

        val metas = org.json.JSONArray()
        db.meta().listAll().forEach { m ->
            metas.put(org.json.JSONObject().apply {
                put("n", m.nombre)
                put("t", m.montoObjetivo)
                put("g0", m.montoJuntado)
                put("f", m.fechaObjetivo)
            })
        }
        root.put("metas", metas)

        val cuentas = org.json.JSONArray()
        db.cuenta().listAll().forEach { c ->
            cuentas.put(org.json.JSONObject().apply {
                put("n", c.nombre)
                put("m", c.saldo)
            })
        }
        root.put("cuentas", cuentas)

        root.put("ajustes", org.json.JSONArray())
        root.put("cierres", org.json.JSONObject())
        return root.toString(2)
    }

    suspend fun importBackupJson(json: String): String {
        val root = org.json.JSONObject(json)
        val nombre = root.optString("nombre", "")
        val sueldo = root.optDouble("sueldo", 0.0)
        val previo = root.optDouble("previo", 0.0)
        val start = root.optString("start", "2026-10-06")
        val end = root.optString("end", "2026-11-04")
        val objetivo = if (root.has("objetivo") && !root.isNull("objetivo")) root.optDouble("objetivo") else null
        val tema = root.optString("tema", "auto").ifBlank { "auto" }

        db.gasto().deleteAll()
        db.gastoImportante().deleteAll()
        db.ingreso().deleteAll()
        db.meta().deleteAll()
        db.cuenta().deleteAll()

        db.settings().save(
            Settings(
                nombre = nombre.ifBlank { "vos" },
                sueldo = sueldo,
                saldoPrevio = previo,
                fechaInicio = start,
                fechaFin = end,
                objetivoPorDia = objetivo,
                tema = tema,
                onboardingHecho = true,
                ultimoBackup = java.time.LocalDate.now().toString()
            )
        )

        val items = root.optJSONArray("items")
        if (items != null) {
            for (i in 0 until items.length()) {
                val o = items.getJSONObject(i)
                val nombreItem = o.optString("n", "").trim()
                if (nombreItem.isEmpty()) continue
                db.gastoImportante().insert(
                    GastoImportante(
                        concepto = nombreItem,
                        monto = o.optDouble("a", 0.0),
                        pagado = o.optBoolean("p", false),
                        montoVariable = o.optBoolean("v", false),
                        repetirMesSiguiente = o.optBoolean("f", false).not() && o.optBoolean("v", false),
                        orden = i
                    )
                )
            }
        }

        val spent = root.optJSONArray("spent")
        var nSpent = 0
        if (spent != null) {
            for (i in 0 until spent.length()) {
                val o = spent.getJSONObject(i)
                val amt = o.optDouble("amt", 0.0)
                // negativos = reintegros / ajustes; los cargamos igual
                db.gasto().insert(
                    Gasto(
                        fecha = o.optString("date", ""),
                        monto = amt,
                        detalle = o.optString("note", ""),
                        categoria = o.optString("cat", "Otro").ifBlank { "Otro" }
                    )
                )
                nSpent++
            }
        }

        val ingresos = root.optJSONArray("ingresos")
        if (ingresos != null) {
            for (i in 0 until ingresos.length()) {
                val o = ingresos.getJSONObject(i)
                db.ingreso().insert(
                    Ingreso(
                        concepto = o.optString("cat", "Ingreso"),
                        monto = o.optDouble("a", 0.0),
                        categoria = o.optString("cat", "Otro ingreso"),
                        orden = i
                    )
                )
            }
        } else if (sueldo > 0) {
            db.ingreso().insert(Ingreso(concepto = "Sueldo", monto = sueldo, categoria = "Sueldo"))
        }

        val metas = root.optJSONArray("metas")
        if (metas != null) {
            for (i in 0 until metas.length()) {
                val o = metas.getJSONObject(i)
                db.meta().insert(
                    Meta(
                        nombre = o.optString("n", "Meta"),
                        montoObjetivo = o.optDouble("t", 0.0),
                        montoJuntado = o.optDouble("g0", 0.0),
                        fechaObjetivo = o.optString("f", "")
                    )
                )
            }
        }

        val cuentas = root.optJSONArray("cuentas")
        if (cuentas != null) {
            for (i in 0 until cuentas.length()) {
                val o = cuentas.getJSONObject(i)
                db.cuenta().insert(
                    Cuenta(
                        nombre = o.optString("n", "Cuenta"),
                        saldo = o.optDouble("m", 0.0),
                        orden = i
                    )
                )
            }
        }

        return "Importados $nSpent gastos diarios y ${items?.length() ?: 0} importantes."
    }

    fun exportCsvDias(dias: List<DiaUi>): String {
        val sb = StringBuilder("fecha,dia,presupuesto,gasto,sobrante\n")
        dias.forEach {
            sb.append("${it.fecha},${it.diaSemana},${it.presupuesto},${it.gastoReal},${it.sobrante}\n")
        }
        return sb.toString()
    }
}
