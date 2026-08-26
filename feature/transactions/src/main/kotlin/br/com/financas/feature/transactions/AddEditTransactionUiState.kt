package br.com.financas.feature.transactions

import br.com.financas.core.model.Category
import br.com.financas.core.model.PaymentMethod
import br.com.financas.core.model.TransactionType

data class AddEditTransactionUiState(
    val amountText: String = "",
    val amountInvalid: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val description: String = "",
    val allCategories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val occurredAt: Long = 0L,
    val isSaving: Boolean = false,
    val isEditing: Boolean = false
) {
    val canSave: Boolean
        get() = amountText.isNotBlank() && !amountInvalid && selectedCategoryId != null && !isSaving
}

/** Evento one-shot — nunca dentro do `UiState` (regra do CLAUDE.md). */
sealed interface AddEditTransactionEvent {
    data object Saved : AddEditTransactionEvent
    data object Deleted : AddEditTransactionEvent
}
