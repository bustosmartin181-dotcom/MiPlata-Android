package com.sueldo.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private val Teal = Color(0xFF0F766E)
private val TealL = Color(0xFF14B8A6)
private val Ok = Color(0xFF059669)
private val OkBg = Color(0xFFD1FAE5)
private val Warn = Color(0xFFD97706)
private val WarnBg = Color(0xFFFEF3C7)
private val Bad = Color(0xFFDC2626)
private val BadBg = Color(0xFFFEE2E2)

private val moneyFmt = NumberFormat.getCurrencyInstance(Locale("es", "AR")).apply {
    maximumFractionDigits = 0; minimumFractionDigits = 0
}
fun money(v: Double, hide: Boolean = false) =
    if (hide) "••••" else try { moneyFmt.format(v) } catch (_: Exception) { "$ ${"%,.0f".format(v)}" }

@Composable
fun MiPlataTheme(tema: String = "auto", content: @Composable () -> Unit) {
    val sysDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dark = when (tema) {
        "oscuro" -> true
        "claro" -> false
        else -> sysDark
    }
    val colors = if (dark) darkColorScheme(primary = TealL, secondary = Color(0xFFF59E0B), background = Color(0xFF0F172A), surface = Color(0xFF1E293B))
    else lightColorScheme(primary = Teal, secondary = Color(0xFFF59E0B), background = Color(0xFFF8FAFC), surface = Color.White)
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun AppNav(vm: MainViewModel) {
    val nav = rememberNavController()
    val back by nav.currentBackStackEntryAsState()
    val dest = back?.destination
    val settings by vm.settings.collectAsState()
    val hoy by vm.resumenHoy.collectAsState()
    val dias by vm.dias.collectAsState()
    val gastos by vm.gastos.collectAsState()
    val importantes by vm.importantes.collectAsState()
    val ingresos by vm.ingresos.collectAsState()
    val cuentas by vm.cuentas.collectAsState()
    val metas by vm.metas.collectAsState()

    // PIN gate
    var unlocked by remember { mutableStateOf(settings.pin.isBlank() || !settings.pinAlAbrir) }
    if (!unlocked && settings.pin.isNotBlank() && settings.pinAlAbrir) {
        PinScreen(settings.pin) { unlocked = true }
        return
    }

    if (!settings.onboardingHecho) {
        OnboardingScreen { vm.saveSettings(settings.copy(onboardingHecho = true, nombre = it)) }
        return
    }

    data class Tab(val r: String, val l: String, val i: androidx.compose.ui.graphics.vector.ImageVector, val o: androidx.compose.ui.graphics.vector.ImageVector)
    val tabs = listOf(
        Tab("hoy", "Hoy", Icons.Filled.Today, Icons.Outlined.Today),
        Tab("dias", "Días", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        Tab("plan", "Plan", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
        Tab("ajustes", "Ajustes", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Scaffold(bottomBar = {
        NavigationBar {
            tabs.forEach { t ->
                val sel = dest?.hierarchy?.any { it.route == t.r } == true
                NavigationBarItem(
                    icon = { Icon(if (sel) t.i else t.o, t.l) },
                    label = { Text(t.l) },
                    selected = sel,
                    onClick = {
                        nav.navigate(t.r) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                )
            }
        }
    }) { pad ->
        NavHost(nav, "hoy", Modifier.padding(pad)) {
            composable("hoy") { HoyScreen(hoy, gastos, cuentas, settings, vm) }
            composable("dias") { DiasScreen(dias, settings, vm) }
            composable("plan") { PlanScreen(importantes, ingresos, metas, settings, vm) }
            composable("ajustes") { AjustesScreen(settings, vm) }
        }
    }
}

@Composable
fun PinScreen(pin: String, onOk: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Lock, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Ingresá tu PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = input, onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) input = it },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = error, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        if (error) Text("PIN incorrecto", color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            if (input == pin) onOk() else error = true
        }, modifier = Modifier.fillMaxWidth()) { Text("Entrar") }
    }
}

@Composable
fun OnboardingScreen(onDone: (String) -> Unit) {
    var page by remember { mutableStateOf(0) }
    var nombre by remember { mutableStateOf("") }
    val pages = listOf(
        "Así funciona" to "Anotás lo que gastás. Lo que sobra pasa a mañana. Si te pasás, se descuenta.",
        "Tu plata, tu decisión" to "Ves cuánto te queda hoy, en el periodo y cómo vas día a día.",
        "De dónde viene tu plata" to "Cargás ingresos y gastos importantes. La app calcula tu día a día.",
        "Tu nombre" to "¿Cómo querés que te digamos?"
    )
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(pages[page].first, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(pages[page].second, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (page == 3) {
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
        Button(
            onClick = { if (page < 3) page++ else onDone(nombre.ifBlank { "vos" }) },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (page < 3) "Siguiente" else "Empezar") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoyScreen(
    hoy: ResumenHoy?,
    gastos: List<Gasto>,
    cuentas: List<Cuenta>,
    settings: Settings,
    vm: MainViewModel
) {
    if (hoy == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    var showAdd by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    var showCuentas by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val hide = hoy.ocultarMontos
    val hoyStr = LocalDate.now().toString()
    val gastosHoy = gastos.filter { it.fecha == hoyStr }
    val ultimos = if (showAll) gastosHoy else gastosHoy.take(5)

    val semColor = when (hoy.semaforo) {
        Semaforo.VERDE -> Ok
        Semaforo.AMARILLO -> Warn
        Semaforo.ROJO -> Bad
    }
    val semBg = when (hoy.semaforo) {
        Semaforo.VERDE -> OkBg
        Semaforo.AMARILLO -> WarnBg
        Semaforo.ROJO -> BadBg
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Anotar gasto") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Hola, ${hoy.nombre}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(hoy.fraseEstado, color = semColor, fontWeight = FontWeight.SemiBold)
                Text(hoy.fraseDelDia, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Mi Plata — Hoy me queda ${money(hoy.sobranteHoy)} de ${money(hoy.presupuestoHoy)}. ${hoy.fraseEstado}")
                    }
                    ctx.startActivity(Intent.createChooser(send, "Compartir resumen"))
                }) { Icon(Icons.Default.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Compartir resumen") }
            }

            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Color.Transparent)) {
                    Box(
                        Modifier.fillMaxWidth().background(
                            Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF0D9488), Color(0xFF14B8A6))),
                            RoundedCornerShape(24.dp)
                        ).padding(22.dp)
                    ) {
                        Column {
                            Text("Te queda para hoy", color = Color.White.copy(0.85f))
                            Text(money(hoy.sobranteHoy, hide), color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { (hoy.gastadoHoy / hoy.presupuestoHoy.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = Color.White, trackColor = Color.White.copy(0.3f)
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Presup. ${money(hoy.presupuestoHoy, hide)}", color = Color.White.copy(0.9f), style = MaterialTheme.typography.bodySmall)
                                Text("Gastado ${money(hoy.gastadoHoy, hide)}", color = Color.White.copy(0.9f), style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(8.dp))
                            Surface(shape = RoundedCornerShape(12.dp), color = semBg) {
                                Text(
                                    "Mañana tendrás ~ ${money(hoy.mananaEstimado, hide)}",
                                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = semColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Racha
            item {
                Text("Racha", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    hoy.racha.takeLast(10).forEach { ok ->
                        Box(Modifier.size(12.dp).clip(CircleShape).background(if (ok) Ok else Bad.copy(0.5f)))
                    }
                    if (hoy.racha.isEmpty()) Text("Anotá gastos para ver tu racha", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Saldo del periodo", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("${hoy.diasRestantes} días · base ${money(hoy.basePorDia, hide)}/día", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (hoy.baseCambiaManana) Text("La base puede cambiar desde mañana", style = MaterialTheme.typography.labelMedium, color = Warn)
                        Text("Queda en el periodo: ${money(hoy.quedaPeriodo, hide)}", fontWeight = FontWeight.Medium)
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Gastos de hoy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { showAll = !showAll }) { Text(if (showAll) "Ver menos" else "Ver todos") }
                }
            }
            items(ultimos, key = { it.id }) { g ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(g.detalle.ifBlank { g.categoria }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(g.categoria, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(money(g.monto, hide), fontWeight = FontWeight.Bold)
                        IconButton(onClick = { vm.deleteGasto(g.id) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                OutlinedButton(onClick = { showCuentas = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AccountBalance, null); Spacer(Modifier.width(8.dp)); Text("Mis cuentas")
                }
                OutlinedButton(onClick = {
                    // Recalibrar: repartir queda periodo / días restantes como ajuste manual
                    if (hoy.diasRestantes > 0) {
                        val nuevo = hoy.quedaPeriodo / hoy.diasRestantes
                        vm.saveSettings(settings.copy(ajusteManualDiario = nuevo))
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Recalibrar días restantes")
                }
                if (settings.ultimoBackup.isBlank()) {
                    Text("Hace tiempo que no guardás una copia de tus datos", style = MaterialTheme.typography.bodySmall, color = Warn)
                }
                Spacer(Modifier.height(72.dp))
            }
        }
    }

    if (showAdd) {
        AddGastoSheet(
            onDismiss = { showAdd = false },
            onSave = { monto, detalle, cat, fecha ->
                vm.addGasto(Gasto(fecha = fecha, monto = monto, detalle = detalle, categoria = cat))
                showAdd = false
            }
        )
    }
    if (showCuentas) {
        CuentasSheet(cuentas, hide, vm) { showCuentas = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGastoSheet(onDismiss: () -> Unit, onSave: (Double, String, String, String) -> Unit) {
    var monto by remember { mutableStateOf("") }
    var detalle by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(Categorias.GASTO.first()) }
    var fecha by remember { mutableStateOf(LocalDate.now().toString()) }
    var vozMsg by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current

    fun parseVoz(text: String) {
        // Ej: "cafe 1500" / "1500 super" / "transporte 2000 nafta"
        val lower = text.lowercase(Locale("es", "AR"))
        val nums = Regex("""(\d+(?:[.,]\d+)?)""").findAll(lower).map { it.value.replace(",", ".") }.toList()
        if (nums.isNotEmpty()) monto = nums.first().substringBefore(".")
        val catHit = Categorias.GASTO.firstOrNull { lower.contains(it.lowercase()) }
        if (catHit != null) cat = catHit
        val sinNum = lower.replace(Regex("""\d+(?:[.,]\d+)?"""), " ").trim()
        val sinCat = if (catHit != null) sinNum.replace(catHit.lowercase(), " ") else sinNum
        val det = sinCat.replace(Regex("""\s+"""), " ").trim()
        if (det.isNotBlank()) detalle = det.replaceFirstChar { it.uppercase() }
        vozMsg = "Escuché: \"$text\""
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val list = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val spoken = list?.firstOrNull()
        if (!spoken.isNullOrBlank()) parseVoz(spoken)
        else vozMsg = "No se entendió. Probá de nuevo."
    }

    val audioPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Decí el gasto, ej: café 1500")
            }
            try {
                speechLauncher.launch(intent)
            } catch (_: Exception) {
                vozMsg = "Reconocimiento de voz no disponible en este dispositivo"
            }
        } else {
            vozMsg = "Necesitamos permiso de micrófono"
        }
    }

    fun startVoz() {
        val ok = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (ok) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Decí el gasto, ej: café 1500")
            }
            try {
                speechLauncher.launch(intent)
            } catch (_: Exception) {
                vozMsg = "Reconocimiento de voz no disponible en este dispositivo"
            }
        } else {
            audioPerm.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            Text("Anotar gasto", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = monto, onValueChange = { monto = it.filter { c -> c.isDigit() || c == '.' || c == ',' } }, label = { Text("Monto") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth(), prefix = { Text("$ ") })
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = detalle, onValueChange = { detalle = it }, label = { Text("Detalle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text("Categoría", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Categorias.GASTO) { c ->
                    FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text(c) })
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = fecha, onValueChange = { fecha = it }, label = { Text("Fecha (AAAA-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { startVoz() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Mic, null)
                Spacer(Modifier.width(8.dp))
                Text("Dictar por voz")
            }
            vozMsg?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val m = monto.replace(",", ".").toDoubleOrNull()
                    if (m != null && m > 0) onSave(m, detalle, cat, fecha)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Guardar") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuentasSheet(cuentas: List<Cuenta>, hide: Boolean, vm: MainViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            Text("Mis cuentas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Compará el saldo real con la app. Ajustar reparte diferencias desde mañana.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            cuentas.forEach { c ->
                var saldo by remember(c.id) { mutableStateOf(c.saldo.toLong().toString()) }
                OutlinedTextField(
                    value = saldo,
                    onValueChange = { saldo = it.filter(Char::isDigit); vm.updateCuenta(c.copy(saldo = saldo.toDoubleOrNull() ?: 0.0)) },
                    label = { Text(c.nombre) },
                    prefix = { Text("$ ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Ajustar con las cuentas") }
        }
    }
}

@Composable
fun DiasScreen(dias: List<DiaUi>, settings: Settings, vm: MainViewModel) {
    var selected by remember { mutableStateOf<DiaUi?>(null) }
    var objetivo by remember { mutableStateOf(settings.objetivoPorDia?.toLong()?.toString() ?: "") }
    val hide = settings.ocultarMontos

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Días", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Mi objetivo por día", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = objetivo,
                        onValueChange = { objetivo = it.filter(Char::isDigit) },
                        label = { Text("Cuánto querés gastar por día") },
                        prefix = { Text("$ ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val obj = objetivo.toDoubleOrNull()
                    if (obj != null && settings.sueldo > 0) {
                        val diasN = dias.size.coerceAtLeast(1)
                        val imp = 0.0 // shown in plan
                        Text("Con ese ritmo usás ~ ${money(obj * diasN, hide)} en el periodo", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            objetivo.toDoubleOrNull()?.let {
                                vm.saveSettings(settings.copy(objetivoPorDia = it, ajusteManualDiario = it))
                            }
                        }) { Text("Usar como mi límite") }
                        OutlinedButton(onClick = {
                            vm.saveSettings(settings.copy(objetivoPorDia = null, ajusteManualDiario = null))
                            objetivo = ""
                        }) { Text("Usar el de la app") }
                    }
                    // Escenarios
                    Spacer(Modifier.height(12.dp))
                    Text("Escenarios", style = MaterialTheme.typography.labelLarge)
                    val base = dias.firstOrNull()?.presupuesto ?: 0.0
                    listOf(0.8, 1.0, 1.2).forEach { f ->
                        val v = (settings.ajusteManualDiario ?: (if (dias.isNotEmpty()) dias.map { it.presupuesto }.average() else 0.0)) * f
                        TextButton(onClick = { objetivo = v.toLong().toString() }) {
                            Text("${(f * 100).toInt()}% · ${money(v, hide)}/día")
                        }
                    }
                }
            }
        }
        items(dias, key = { it.fecha.toString() }) { d ->
            Card(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.clickable { selected = d },
                colors = CardDefaults.cardColors(
                    if (d.esHoy) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (d.cerrado) Icon(Icons.Default.Lock, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${d.diaSemana} ${d.fecha.dayOfMonth}/${d.fecha.monthValue}", fontWeight = FontWeight.SemiBold)
                        Text("P ${money(d.presupuesto, hide)} · G ${money(d.gastoReal, hide)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        money(d.sobrante, hide),
                        fontWeight = FontWeight.Bold,
                        color = if (d.sobrante >= 0) Ok else Bad
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    selected?.let { d ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("${d.diaSemana} ${d.fecha}") },
            text = {
                Column {
                    Text("Presupuesto: ${money(d.presupuesto, hide)}")
                    Text("Gastado: ${money(d.gastoReal, hide)}")
                    Text("Sobrante: ${money(d.sobrante, hide)}")
                    Spacer(Modifier.height(8.dp))
                    d.gastos.forEach {
                        Text("· ${it.detalle.ifBlank { it.categoria }}: ${money(it.monto, hide)}")
                    }
                    if (d.gastos.isEmpty()) Text("Sin gastos anotados")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.setDiaCerrado(d.fecha.toString(), !d.cerrado)
                    selected = null
                }) { Text(if (d.cerrado) "Abrir día" else "Cerrar día") }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Cerrar") } }
        )
    }
}

@Composable
fun PlanScreen(
    importantes: List<GastoImportante>,
    ingresos: List<Ingreso>,
    metas: List<Meta>,
    settings: Settings,
    vm: MainViewModel
) {
    val hide = settings.ocultarMontos
    val totalIng = ingresos.sumOf { it.monto }.let { if (it == 0.0) settings.sueldo else it }
    val totalImp = importantes.sumOf { it.monto }
    val pagado = importantes.filter { it.pagado }.sumOf { it.monto }
    val falta = totalImp - pagado
    val disponible = totalIng + settings.saldoPrevio - totalImp

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Plan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Resumen del mes", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    RowLine("Ingresos", money(totalIng, hide))
                    RowLine("Gastos importantes", money(totalImp, hide))
                    RowLine("Falta pagar", money(falta, hide), if (falta == 0.0) Ok else Bad)
                    RowLine("Disponible día a día", money(disponible, hide), Ok)
                }
            }
        }
        item { Text("Ingresos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        items(ingresos, key = { it.id }) { i ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(i.concepto, fontWeight = FontWeight.SemiBold)
                        Text(i.categoria, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(money(i.monto, hide), fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            OutlinedButton(onClick = {
                vm.addIngreso(Ingreso(concepto = "Nuevo ingreso", monto = 0.0, categoria = "Otro ingreso"))
            }, modifier = Modifier.fillMaxWidth()) { Text("+ Agregar ingreso") }
        }
        item { Text("Pagar al cobrar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        items(importantes, key = { it.id }) { g ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.toggleImportante(g) }) {
                        Icon(if (g.pagado) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null, tint = if (g.pagado) Ok else MaterialTheme.colorScheme.outline)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(g.concepto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(money(g.monto, hide), style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (g.repetirMesSiguiente) AssistChip(onClick = {}, label = { Text("Fijo") })
                            if (g.montoVariable) AssistChip(onClick = {}, label = { Text("Variable") })
                            if (g.enCuotas) AssistChip(onClick = {}, label = { Text("${g.cuotaActual}/${g.cuotaTotal}") })
                        }
                    }
                    Surface(shape = RoundedCornerShape(50), color = if (g.pagado) OkBg else BadBg) {
                        Text(if (g.pagado) "Pagado" else "Pendiente", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = if (g.pagado) Color(0xFF065F46) else Color(0xFF991B1B), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        item { Text("Metas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        items(metas, key = { it.id }) { m ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(m.nombre, fontWeight = FontWeight.SemiBold)
                    Text("${money(m.montoJuntado, hide)} de ${money(m.montoObjetivo, hide)}", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { (m.montoJuntado / m.montoObjetivo.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    if (m.fechaObjetivo.isNotBlank()) Text("Objetivo: ${m.fechaObjetivo}", style = MaterialTheme.typography.bodySmall)
                    val meses = 3.0
                    val aporte = ((m.montoObjetivo - m.montoJuntado) / meses).coerceAtLeast(0.0)
                    Text("Aporte sugerido: ${money(aporte, hide)}/mes", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { vm.updateMeta(m.copy(montoJuntado = m.montoJuntado + aporte)) }) { Text("Aportar") }
                }
            }
        }
        item {
            OutlinedButton(onClick = {
                vm.addMeta(Meta(nombre = "Nueva meta", montoObjetivo = 100000.0, fechaObjetivo = LocalDate.now().plusMonths(3).toString()))
            }, modifier = Modifier.fillMaxWidth()) { Text("+ Nueva meta") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RowLine(label: String, value: String, color: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color)
    }
}

@Composable
fun AjustesScreen(settings: Settings, vm: MainViewModel) {
    var nombre by remember(settings) { mutableStateOf(settings.nombre) }
    var sueldo by remember(settings) { mutableStateOf(settings.sueldo.toLong().toString()) }
    var saldo by remember(settings) { mutableStateOf(settings.saldoPrevio.toLong().toString()) }
    var pin by remember(settings) { mutableStateOf(settings.pin) }
    var hora by remember(settings) { mutableStateOf(settings.recordatorioHora) }
    var msg by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    val dias by vm.dias.collectAsState()
    val gastos by vm.gastos.collectAsState()
    val importantes by vm.importantes.collectAsState()

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }

        item {
            Section("Perfil y periodo") {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Tu nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = sueldo, onValueChange = { sueldo = it.filter(Char::isDigit) }, label = { Text("Sueldo") }, prefix = { Text("$ ") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = saldo, onValueChange = { saldo = it.filter(Char::isDigit) }, label = { Text("Saldo previo") }, prefix = { Text("$ ") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Periodo: ${settings.fechaInicio} → ${settings.fechaFin}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Aviso al ${settings.avisoPorcentaje}% del presupuesto del día", style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
            Section("PIN de seguridad") {
                OutlinedTextField(value = pin, onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it }, label = { Text("PIN (4-8 números, vacío = sin PIN)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(settings.pinAlAbrir, onCheckedChange = { vm.saveSettings(settings.copy(pinAlAbrir = it)) })
                    Text("Pedir al abrir la app")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(settings.pinSoloMontos, onCheckedChange = { vm.saveSettings(settings.copy(pinSoloMontos = it)) })
                    Text("Solo para mostrar montos")
                }
            }
        }

        item {
            Section("Apariencia") {
                Text("Tema")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("auto" to "Auto", "claro" to "Claro", "oscuro" to "Oscuro").forEach { (k, l) ->
                        FilterChip(selected = settings.tema == k, onClick = { vm.saveSettings(settings.copy(tema = k)) }, label = { Text(l) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(settings.ocultarMontos, onCheckedChange = { vm.saveSettings(settings.copy(ocultarMontos = it)) })
                    Text("Ocultar montos con puntitos")
                }
            }
        }

        item {
            Section("Recordatorio diario") {
                OutlinedTextField(
                    value = hora,
                    onValueChange = { hora = it },
                    label = { Text("Hora (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = settings.recordatorioActivo,
                        onCheckedChange = { checked ->
                            val s = settings.copy(recordatorioActivo = checked, recordatorioHora = hora)
                            vm.saveSettings(s)
                            Notif.scheduleDaily(ctx, hora, checked)
                            msg = if (checked) "Recordatorio activado a las $hora" else "Recordatorio desactivado"
                        }
                    )
                    Text("Activar notificación diaria")
                }
                Button(
                    onClick = {
                        val s = settings.copy(recordatorioActivo = true, recordatorioHora = hora)
                        vm.saveSettings(s)
                        Notif.scheduleDaily(ctx, hora, true)
                        msg = "Recordatorio programado a las $hora"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Aplicar hora del recordatorio") }
            }
        }

        item {
            Section("Datos y copias") {
                Text("Backup cada ${settings.backupCadaDias} días · Último: ${settings.ultimoBackup.ifBlank { "nunca" }}")
                Button(onClick = {
                    vm.saveSettings(settings.copy(ultimoBackup = LocalDate.now().toString()))
                    msg = "Copia marcada (${LocalDate.now()})"
                }, modifier = Modifier.fillMaxWidth()) { Text("Guardar copia ahora") }
                OutlinedButton(onClick = {
                    ExportCsv.share(ctx, "mi_plata_dias", ExportCsv.diasCsv(dias))
                    msg = "Exportando días (CSV)…"
                }, modifier = Modifier.fillMaxWidth()) { Text("Exportar días CSV") }
                OutlinedButton(onClick = {
                    ExportCsv.share(ctx, "mi_plata_gastos", ExportCsv.gastosCsv(gastos))
                    msg = "Exportando gastos (CSV)…"
                }, modifier = Modifier.fillMaxWidth()) { Text("Exportar gastos CSV") }
                OutlinedButton(onClick = {
                    ExportCsv.share(ctx, "mi_plata_importantes", ExportCsv.importantesCsv(importantes))
                    msg = "Exportando gastos importantes (CSV)…"
                }, modifier = Modifier.fillMaxWidth()) { Text("Exportar importantes CSV") }
                OutlinedButton(onClick = {
                    msg = "Nuevo periodo: se conservan gastos con repetición; el actual pasa al historial."
                    vm.saveSettings(settings.copy(fechaInicio = LocalDate.now().toString(), fechaFin = LocalDate.now().plusDays(29).toString(), saldoPrevio = 0.0))
                }, modifier = Modifier.fillMaxWidth()) { Text("Nuevo periodo") }
                OutlinedButton(onClick = {
                    msg = "Cierre nocturno: congela el día y guarda copia restaurable (lógica lista en modelo DiaEstado)."
                }, modifier = Modifier.fillMaxWidth()) { Text("Cierre nocturno (info)") }
            }
        }

        item {
            Section("Otros") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(settings.demoMode, onCheckedChange = { vm.saveSettings(settings.copy(demoMode = it)) })
                    Text("Modo demo")
                }
                OutlinedButton(onClick = {
                    vm.saveSettings(Settings(onboardingHecho = false))
                    msg = "Datos reiniciados al próximo inicio (seed)."
                }, modifier = Modifier.fillMaxWidth()) { Text("Empezar de cero") }
                Text("Mi Plata 10.0.0 · Kotlin · Compose", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Acerca de: control de sueldo y gastos diarios. Tu plata, tu decisión.", style = MaterialTheme.typography.bodySmall)
            }
        }

        item {
            Button(
                onClick = {
                    vm.saveSettings(
                        settings.copy(
                            nombre = nombre,
                            sueldo = sueldo.toDoubleOrNull() ?: settings.sueldo,
                            saldoPrevio = saldo.toDoubleOrNull() ?: settings.saldoPrevio,
                            pin = pin
                        )
                    )
                    msg = "Guardado"
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Guardar cambios") }
            msg?.let { Text(it, color = Ok) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
