package com.sueldo.app.ui.ajustes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sueldo.app.data.Config
import com.sueldo.app.ui.components.SectionCard

@Composable
fun AjustesScreen(
    config: Config?,
    onSave: (Config) -> Unit,
    modifier: Modifier = Modifier
) {
    if (config == null) return

    var sueldo by remember(config) { mutableStateOf(config.sueldo.toLong().toString()) }
    var saldo by remember(config) { mutableStateOf(config.saldoPrevio.toLong().toString()) }
    var ajuste by remember(config) {
        mutableStateOf(config.ajusteManualDiario?.toLong()?.toString() ?: "")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configurá tu sueldo y período",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        SectionCard {
            Column {
                Text(
                    text = "Ingresos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = sueldo,
                    onValueChange = { sueldo = it.filter { c -> c.isDigit() } },
                    label = { Text("Sueldo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    prefix = { Text("$ ") }
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = saldo,
                    onValueChange = { saldo = it.filter { c -> c.isDigit() } },
                    label = { Text("Saldo previo en cuenta") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    prefix = { Text("$ ") }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionCard {
            Column {
                Text(
                    text = "Ajuste manual diario (opcional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Dejá vacío para usar el cálculo automático. Útil si querés fijar un monto diario distinto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = ajuste,
                    onValueChange = { ajuste = it.filter { c -> c.isDigit() } },
                    label = { Text("Monto diario fijo") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    prefix = { Text("$ ") }
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                val newConfig = config.copy(
                    sueldo = sueldo.toDoubleOrNull() ?: config.sueldo,
                    saldoPrevio = saldo.toDoubleOrNull() ?: config.saldoPrevio,
                    ajusteManualDiario = ajuste.toDoubleOrNull()
                )
                onSave(newConfig)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Guardar cambios", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(40.dp))
    }
}
