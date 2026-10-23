package com.sueldo.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SueldoApp : Application() {
    lateinit var repository: Repository
        private set
    override fun onCreate() {
        super.onCreate()
        repository = Repository(AppDatabase.get(this))
        Notif.createChannel(this)
    }
}

class MainViewModel(private val repo: Repository) : ViewModel() {
    val settings: StateFlow<Settings> = repo.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())
    val resumenHoy: StateFlow<ResumenHoy?> = repo.resumenHoyFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val dias: StateFlow<List<DiaUi>> = repo.diasUiFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val gastos: StateFlow<List<Gasto>> = repo.gastos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val importantes: StateFlow<List<GastoImportante>> = repo.gastosImportantes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val ingresos: StateFlow<List<Ingreso>> = repo.ingresos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cuentas: StateFlow<List<Cuenta>> = repo.cuentas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val metas: StateFlow<List<Meta>> = repo.metas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { viewModelScope.launch { repo.seedIfNeeded() } }

    fun saveSettings(s: Settings) = viewModelScope.launch { repo.saveSettings(s) }
    fun addGasto(g: Gasto) = viewModelScope.launch { repo.addGasto(g) }
    fun deleteGasto(id: Long) = viewModelScope.launch { repo.deleteGasto(id) }
    fun toggleImportante(g: GastoImportante) = viewModelScope.launch { repo.updateImportante(g.copy(pagado = !g.pagado)) }
    fun addIngreso(i: Ingreso) = viewModelScope.launch { repo.addIngreso(i) }
    fun addMeta(m: Meta) = viewModelScope.launch { repo.addMeta(m) }
    fun updateMeta(m: Meta) = viewModelScope.launch { repo.updateMeta(m) }
    fun updateCuenta(c: Cuenta) = viewModelScope.launch { repo.updateCuenta(c) }
    fun setDiaCerrado(fecha: String, cerrado: Boolean) = viewModelScope.launch { repo.setDiaCerrado(fecha, cerrado) }

    class Factory(private val repo: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repo) as T
    }
}

class MainActivity : ComponentActivity() {
    private val requestNotif = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val app = application as SueldoApp
        setContent {
            val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(app.repository))
            val settings by vm.settings.collectAsState()
            MiPlataTheme(tema = settings.tema) {
                Surface(Modifier.fillMaxSize()) {
                    AppNav(vm)
                }
            }
        }
    }
}
