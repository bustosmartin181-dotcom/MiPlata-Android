package com.sueldo.app.ui.diario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sueldo.app.data.Dia
import com.sueldo.app.data.DiaUi
import com.sueldo.app.ui.components.MoneyText
import com.sueldo.app.ui.components.ProgressBarCustom
import com.sueldo.app.ui.components.SectionCard
import com.sueldo.app.ui.components.formatMoney
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiarioScreen(
    dias: List<DiaUi>,
    onSaveDia: (Dia) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf<DiaUi?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    // Auto-select today or first day
    LaunchedEffect(dias) {
        if (selected == null && dias.isNotEmpty()) {
            selected = dias.find { it.esHoy } ?: dias.first()
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(dias, selected) {
        val idx = dias.indexOfFirst { it.fecha == selected?.fecha }
        if (idx >= 0) listState.animateScrollToItem(maxOf(0, idx - 2))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Diario",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Text(
            text = "El sobrante pasa al día siguiente",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(Modifier.height(16.dp))

        // Horizontal date strip
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(dias, key = { it.fecha.toString() }) { dia ->
                DateChip(
                    dia = dia,
                    selected = dia.fecha == selected?.fecha,
                    onClick = { selected = dia }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        selected?.let { dia ->
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DayDetailCard(
                        dia = dia,
                        onEdit = { showEditor = true }
                    )
                }

                // Recent history
                item {
                    Text(
                        text = "Últimos días",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(dias.filter { it.gastoReal != null }.takeLast(7).reversed()) { d ->
                    MiniDayRow(d)
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }

    if (showEditor && selected != null) {
        EditDaySheet(
            dia = selected!!,
            onDismiss = { showEditor = false },
            onSave = { gasto, nota ->
                onSaveDia(
                    Dia(
                        fecha = selected!!.fecha.toString(),
                        gastoReal = gasto,
                        nota = nota
                    )
                )
                showEditor = false
            }
        )
    }
}

@Composable
private fun DateChip(
    dia: DiaUi,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        selected -> MaterialTheme.colorScheme.primary
        dia.esHoy -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val fg = when {
        selected -> Color.White
        dia.esHoy -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .width(56.dp)
    ) {
        Text(
            text = dia.fecha.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = fg
        )
        Text(
            text = dia.diaSemana.take(3).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg.copy(alpha = 0.8f)
        )
        if (dia.gastoReal != null) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) Color.White
                        else if ((dia.sobrante ?: 0.0) >= 0) Color(0xFF059669)
                        else MaterialTheme.colorScheme.error
                    )
            )
        }
    }
}

@Composable
private fun DayDetailCard(
    dia: DiaUi,
    onEdit: () -> Unit
) {
    val gastado = dia.gastoReal ?: 0.0
    val progress = if (dia.presupuesto > 0) (gastado / dia.presupuesto).toFloat() else 0f
    val sobrante = dia.sobrante

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${dia.diaSemana}, ${dia.fecha.dayOfMonth} ${
                            dia.fecha.month.getDisplayName(
                                java.time.format.TextStyle.FULL,
                                Locale("es", "AR")
                            )
                        }",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (dia.esHoy) {
                        Text(
                            text = "Hoy",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(onClick = onEdit)
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Presupuesto",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MoneyText(amount = dia.presupuesto, style = MaterialTheme.typography.headlineSmall)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Gastado",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    MoneyText(
                        amount = gastado,
                        style = MaterialTheme.typography.headlineSmall,
                        positiveIsGood = false
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            ProgressBarCustom(
                progress = progress,
                color = when {
                    progress > 1f -> MaterialTheme.colorScheme.error
                    progress > 0.8f -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            Spacer(Modifier.height(16.dp))

            if (sobrante != null) {
                val isPositive = sobrante >= 0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isPositive) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isPositive) "Sobrante → mañana" else "Exceso",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isPositive) Color(0xFF065F46) else Color(0xFF991B1B)
                        )
                        Text(
                            text = formatMoney(sobrante),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) Color(0xFF065F46) else Color(0xFF991B1B)
                        )
                    }
                }
            } else {
                Text(
                    text = "Todavía no cargaste el gasto de este día",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (dia.nota.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Nota: ${dia.nota}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MiniDayRow(dia: DiaUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${dia.fecha.dayOfMonth}/${dia.fecha.monthValue}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = formatMoney(dia.gastoReal ?: 0.0),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        dia.sobrante?.let {
            Text(
                text = formatMoney(it),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (it >= 0) Color(0xFF059669) else MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDaySheet(
    dia: DiaUi,
    onDismiss: () -> Unit,
    onSave: (Double?, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var gastoText by remember { mutableStateOf(dia.gastoReal?.toInt()?.toString() ?: "") }
    var nota by remember { mutableStateOf(dia.nota) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text = "Registrar gasto",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${dia.diaSemana} ${dia.fecha.dayOfMonth}/${dia.fecha.monthValue}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = gastoText,
                onValueChange = { gastoText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                label = { Text("Gasto real ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = nota,
                onValueChange = { nota = it },
                label = { Text("Nota (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    val value = gastoText.replace(",", ".").toDoubleOrNull()
                    onSave(value, nota)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Guardar", style = MaterialTheme.typography.titleMedium)
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    }
}
