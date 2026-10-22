package com.sueldo.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class Repository(
    private val configDao: ConfigDao,
    private val gastoDao: GastoImportanteDao,
    private val diaDao: DiaDao
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val locale = Locale("es", "AR")

    val config: Flow<Config> = configDao.getConfig().map { it ?: Config() }
    val gastos: Flow<List<GastoImportante>> = gastoDao.getAll()
    val dias: Flow<List<Dia>> = diaDao.getAll()

    suspend fun saveConfig(config: Config) = configDao.save(config)
    suspend fun updateGasto(gasto: GastoImportante) = gastoDao.update(gasto)
    suspend fun insertGasto(gasto: GastoImportante) = gastoDao.insert(gasto)
    suspend fun deleteGasto(id: Long) = gastoDao.delete(id)
    suspend fun upsertDia(dia: Dia) = diaDao.upsert(dia)

    fun resumenFlow(): Flow<ResumenUi> = combine(config, gastos, dias) { cfg, gastosList, diasList ->
        calcularResumen(cfg, gastosList, diasList)
    }

    fun diasUiFlow(): Flow<List<DiaUi>> = combine(config, gastos, dias) { cfg, gastosList, diasList ->
        calcularDias(cfg, gastosList, diasList)
    }

    private fun calcularResumen(
        cfg: Config,
        gastosList: List<GastoImportante>,
        diasList: List<Dia>
    ): ResumenUi {
        val totalGastos = gastosList.sumOf { it.monto }
        val totalPagado = gastosList.filter { it.pagado }.sumOf { it.monto }
        val porPagar = totalGastos - totalPagado
        val totalDisponible = cfg.sueldo + cfg.saldoPrevio
        val disponibleDiario = totalDisponible - totalGastos

        val inicio = LocalDate.parse(cfg.fechaInicio)
        val fin = LocalDate.parse(cfg.fechaFin)
        val diasTotales = (fin.toEpochDay() - inicio.toEpochDay() + 1).toInt()
        val hoy = LocalDate.now()
        val diasTranscurridos = when {
            hoy.isBefore(inicio) -> 0
            hoy.isAfter(fin) -> diasTotales
            else -> (hoy.toEpochDay() - inicio.toEpochDay()).toInt()
        }
        val diasRestantes = (diasTotales - diasTranscurridos).coerceAtLeast(0)

        val montoBase = if (diasTotales > 0) disponibleDiario / diasTotales else 0.0
        val montoRige = cfg.ajusteManualDiario ?: montoBase

        val totalGastadoDiario = diasList.mapNotNull { it.gastoReal }.sum()
        val queda = disponibleDiario - totalGastadoDiario

        return ResumenUi(
            sueldo = cfg.sueldo,
            saldoPrevio = cfg.saldoPrevio,
            totalDisponible = totalDisponible,
            totalGastosImportantes = totalGastos,
            totalPagado = totalPagado,
            todaviaPorPagar = porPagar,
            disponibleDiario = disponibleDiario,
            montoBasePorDia = montoBase,
            montoDiarioQueRige = montoRige,
            diasTotales = diasTotales,
            diasTranscurridos = diasTranscurridos,
            diasRestantes = diasRestantes,
            totalGastadoDiario = totalGastadoDiario,
            quedaDelDisponible = queda
        )
    }

    private fun calcularDias(
        cfg: Config,
        gastosList: List<GastoImportante>,
        diasList: List<Dia>
    ): List<DiaUi> {
        val resumen = calcularResumen(cfg, gastosList, diasList)
        val inicio = LocalDate.parse(cfg.fechaInicio)
        val fin = LocalDate.parse(cfg.fechaFin)
        val mapa = diasList.associateBy { it.fecha }
        val hoy = LocalDate.now()

        val lista = mutableListOf<DiaUi>()
        var carry = 0.0
        var current = inicio

        while (!current.isAfter(fin)) {
            val fechaStr = current.format(dateFmt)
            val registro = mapa[fechaStr]
            val presupuesto = resumen.montoDiarioQueRige + carry
            val gasto = registro?.gastoReal
            val sobrante = if (gasto != null) presupuesto - gasto else null

            lista.add(
                DiaUi(
                    fecha = current,
                    diaSemana = current.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
                        .replaceFirstChar { it.uppercase() },
                    presupuesto = presupuesto,
                    gastoReal = gasto,
                    sobrante = sobrante,
                    nota = registro?.nota ?: "",
                    esHoy = current == hoy,
                    esPasado = current.isBefore(hoy)
                )
            )

            carry = sobrante ?: 0.0
            current = current.plusDays(1)
        }
        return lista
    }

    suspend fun seedIfNeeded() {
        if (gastoDao.count() == 0) {
            val defaults = listOf(
                GastoImportante(concepto = "Telecom personal", monto = 57_172.0, pagado = true, orden = 0),
                GastoImportante(concepto = "Préstamo Maríela", monto = 260_000.0, pagado = true, orden = 1),
                GastoImportante(concepto = "Alejandro Bustos", monto = 138_310.0, pagado = true, orden = 2),
                GastoImportante(concepto = "Musimundo", monto = 77_302.0, pagado = true, orden = 3),
                GastoImportante(concepto = "Seguro moto", monto = 30_616.0, pagado = true, orden = 4),
                GastoImportante(concepto = "Wiber internet", monto = 53_905.0, pagado = true, orden = 5),
                GastoImportante(concepto = "Reserva supermercado", monto = 200_000.0, pagado = true, orden = 6),
                GastoImportante(concepto = "Debo a Orlando", monto = 50_000.0, pagado = true, orden = 7),
                GastoImportante(concepto = "Reserva asados trabajo", monto = 60_000.0, pagado = true, orden = 8),
                GastoImportante(concepto = "Tarjeta Naranja", monto = 217_109.0, pagado = true, orden = 9),
                GastoImportante(concepto = "Nafta", monto = 90_412.0, pagado = true, orden = 10),
                GastoImportante(concepto = "Posible desagote pluvial", monto = 40_000.0, pagado = true, orden = 11),
            )
            defaults.forEach { gastoDao.insert(it) }
            configDao.save(Config())
        }
    }
}
