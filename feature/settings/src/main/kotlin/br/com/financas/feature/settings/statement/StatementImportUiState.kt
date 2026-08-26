package br.com.financas.feature.settings.statement

import br.com.financas.core.model.StatementPreviewItem

sealed interface StatementImportUiState {
    data object Idle : StatementImportUiState
    data object Loading : StatementImportUiState
    data class Preview(
        val items: List<StatementPreviewItem>,
        val selectedExternalIds: Set<String>
    ) : StatementImportUiState {
        val selectedCount: Int get() = selectedExternalIds.size
    }
    data class Done(val importedCount: Int) : StatementImportUiState
    data class Error(val message: String) : StatementImportUiState
}
