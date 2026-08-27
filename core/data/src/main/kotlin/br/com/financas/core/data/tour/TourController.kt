package br.com.financas.core.data.tour

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orquestra o tour guiado — `:app` observa [currentStep] para decidir quando navegar e o
 * que desenhar no overlay; este controller não sabe nada sobre telas ou UI.
 */
@Singleton
class TourController @Inject constructor(
    private val preferences: TourPreferences
) {
    // Escopo de aplicação — mesmo padrão de FinanceApp.applicationScope; o controller
    // vive durante todo o processo, não há um lifecycle específico para cancelar.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentStep = MutableStateFlow<TourStep?>(null)
    val currentStep: StateFlow<TourStep?> = _currentStep.asStateFlow()

    fun start() {
        _currentStep.value = TourStep.entries.first()
    }

    fun next() {
        val steps = TourStep.entries
        val nextIndex = steps.indexOf(_currentStep.value) + 1
        _currentStep.value = steps.getOrNull(nextIndex)
        if (_currentStep.value == null) markCompleted()
    }

    fun skip() {
        _currentStep.value = null
        markCompleted()
    }

    private fun markCompleted() {
        scope.launch { preferences.markCompleted() }
    }
}
