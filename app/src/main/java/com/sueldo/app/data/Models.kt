package com.sueldo.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "config")
data class Config(
    @PrimaryKey val id: Int = 1,
    val sueldo: Double = 1_847_755.62,
    val saldoPrevio: Double = 26_255.87,
    val fechaInicio: String = "2026-10-06",
    val fechaFin: String = "2026-11-04",
    val ajusteManualDiario: Double? = null
)

@Entity(tableName = "gasto_importante")
data class GastoImportante(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val concepto: String,
    val monto: Double,
    val pagado: Boolean = false,
    val orden: Int = 0
)

@Entity(tableName = "dia")
data class Dia(
    @PrimaryKey val fecha: String, // yyyy-MM-dd
    val gastoReal: Double? = null,
    val nota: String = ""
)

// UI models
data class ResumenUi(
    val sueldo: Double,
    val saldoPrevio: Double,
    val totalDisponible: Double,
    val totalGastosImportantes: Double,
    val totalPagado: Double,
    val todaviaPorPagar: Double,
    val disponibleDiario: Double,
    val montoBasePorDia: Double,
    val montoDiarioQueRige: Double,
    val diasTotales: Int,
    val diasTranscurridos: Int,
    val diasRestantes: Int,
    val totalGastadoDiario: Double,
    val quedaDelDisponible: Double
)

data class DiaUi(
    val fecha: LocalDate,
    val diaSemana: String,
    val presupuesto: Double,
    val gastoReal: Double?,
    val sobrante: Double?,
    val nota: String,
    val esHoy: Boolean,
    val esPasado: Boolean
)
