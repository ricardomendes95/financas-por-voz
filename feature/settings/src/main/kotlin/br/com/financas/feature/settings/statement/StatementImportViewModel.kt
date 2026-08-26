package br.com.financas.feature.settings.statement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.financas.core.data.repository.StatementImportRepository
import br.com.financas.core.data.statement.NubankCsvStatementParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatementImportViewModel @Inject constructor(
    private val repository: StatementImportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatementImportUiState>(StatementImportUiState.Idle)
    val uiState: StateFlow<StatementImportUiState> = _uiState.asStateFlow()

    fun onFileSelected(content: String) {
        _uiState.value = StatementImportUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val entries = NubankCsvStatementParser.parse(content)
            if (entries.isEmpty()) {
                _uiState.value = StatementImportUiState.Error("Não encontrei lançamentos nesse arquivo.")
                return@launch
            }
            val items = repository.preview(entries)
            val defaultSelected = items.filterNot { it.alreadyImported }.map { it.entry.externalId }.toSet()
            _uiState.value = StatementImportUiState.Preview(items, defaultSelected)
        }
    }

    fun onReadFailed() {
        _uiState.value = StatementImportUiState.Error("Não foi possível ler esse arquivo.")
    }

    fun onToggleSelection(externalId: String) {
        _uiState.update { state ->
            if (state !is StatementImportUiState.Preview) return@update state
            val newSelection = if (externalId in state.selectedExternalIds) {
                state.selectedExternalIds - externalId
            } else {
                state.selectedExternalIds + externalId
            }
            state.copy(selectedExternalIds = newSelection)
        }
    }

    fun onConfirmImport() {
        val state = _uiState.value as? StatementImportUiState.Preview ?: return
        val selectedItems = state.items.filter { it.entry.externalId in state.selectedExternalIds }
        _uiState.value = StatementImportUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.confirmImport(selectedItems)
            _uiState.value = StatementImportUiState.Done(count)
        }
    }

    fun onReset() {
        _uiState.value = StatementImportUiState.Idle
    }
}
