package com.sueldo.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sueldo.app.data.Config
import com.sueldo.app.data.Dia
import com.sueldo.app.data.GastoImportante
import com.sueldo.app.data.Repository
import com.sueldo.app.data.ResumenUi
import com.sueldo.app.data.DiaUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repo: Repository) : ViewModel() {

    val resumen: StateFlow<ResumenUi?> = repo.resumenFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val gastos: StateFlow<List<GastoImportante>> = repo.gastos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dias: StateFlow<List<DiaUi>> = repo.diasUiFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val config: StateFlow<Config?> = repo.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            repo.seedIfNeeded()
        }
    }

    fun togglePagado(gasto: GastoImportante) {
        viewModelScope.launch {
            repo.updateGasto(gasto.copy(pagado = !gasto.pagado))
        }
    }

    fun saveDia(dia: Dia) {
        viewModelScope.launch {
            repo.upsertDia(dia)
        }
    }

    fun saveConfig(config: Config) {
        viewModelScope.launch {
            repo.saveConfig(config)
        }
    }

    fun addGasto(concepto: String, monto: Double) {
        viewModelScope.launch {
            val maxOrden = gastos.value.maxOfOrNull { it.orden } ?: -1
            repo.insertGasto(
                GastoImportante(
                    concepto = concepto,
                    monto = monto,
                    pagado = false,
                    orden = maxOrden + 1
                )
            )
        }
    }

    class Factory(private val repo: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repo) as T
        }
    }
}
