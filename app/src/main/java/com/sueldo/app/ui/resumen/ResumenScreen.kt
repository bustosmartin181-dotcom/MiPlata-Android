package com.sueldo.app.ui.resumen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sueldo.app.data.ResumenUi
import com.sueldo.app.ui.components.HeroCard
import com.sueldo.app.ui.components.ProgressBarCustom
import com.sueldo.app.ui.components.SectionCard
import com.sueldo.app.ui.components.StatMiniCard
import com.sueldo.app.ui.components.formatMoney
import com.sueldo.app.ui.components.formatMoneyShort

@Composable
fun ResumenScreen(
    resumen: ResumenUi?,
    modifier: Modifier = Modifier
) {
    if (resumen == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Cargando…", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val progreso = if (resumen.diasTotales > 0) {
        resumen.diasTranscurridos.toFloat() / resumen.diasTotales
    } else 0f

    val ritmoOk = resumen.totalGastadoDiario <= (resumen.montoDiarioQueRige * resumen.diasTranscurridos.coerceAtLeast(1))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Resumen del período",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "6 oct → 4 nov · ${resumen.diasRestantes} días restantes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        HeroCard(
            title = "Disponible para el día a día",
            amount = resumen.quedaDelDisponible,
            subtitle = if (ritmoOk) "Vas bien con el ritmo de gasto"
            else "Cuidado: estás gastando más rápido de lo planeado"
        )

        Spacer(Modifier.height(16.dp))

        // Progress del período
        SectionCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Avance del período",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${(progreso * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
                ProgressBarCustom(progress = progreso)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${resumen.diasTranscurridos} de ${resumen.diasTotales} días",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Mini stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatMiniCard(
                label = "Sueldo",
                value = formatMoneyShort(resumen.sueldo),
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatMiniCard(
                label = "Saldo previo",
                value = formatMoneyShort(resumen.saldoPrevio),
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatMiniCard(
                label = "Gastos fijos",
                value = formatMoneyShort(resumen.totalGastosImportantes),
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            StatMiniCard(
                label = "Por día",
                value = formatMoneyShort(resumen.montoDiarioQueRige),
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Detalle de gastos fijos
        SectionCard {
            Column {
                Text(
                    text = "Gastos importantes",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(16.dp))

                DetailRow("Total", formatMoney(resumen.totalGastosImportantes))
                DetailRow("Ya pagado", formatMoney(resumen.totalPagado), good = true)
                DetailRow(
                    "Todavía por pagar",
                    formatMoney(resumen.todaviaPorPagar),
                    good = resumen.todaviaPorPagar == 0.0
                )

                Spacer(Modifier.height(12.dp))
                ProgressBarCustom(
                    progress = if (resumen.totalGastosImportantes > 0)
                        (resumen.totalPagado / resumen.totalGastosImportantes).toFloat()
                    else 1f,
                    color = if (resumen.todaviaPorPagar == 0.0)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Diario
        SectionCard {
            Column {
                Text(
                    text = "Control diario",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(16.dp))
                DetailRow("Gastado hasta ahora", formatMoney(resumen.totalGastadoDiario))
                DetailRow(
                    "Queda del disponible",
                    formatMoney(resumen.quedaDelDisponible),
                    good = resumen.quedaDelDisponible >= 0
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    good: Boolean? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = when (good) {
                true -> Color(0xFF059669)
                false -> MaterialTheme.colorScheme.error
                null -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

