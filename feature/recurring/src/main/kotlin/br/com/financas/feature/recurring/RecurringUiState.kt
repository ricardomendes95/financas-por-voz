package br.com.financas.feature.recurring

import br.com.financas.core.model.Category
import br.com.financas.core.model.TransactionType

data class RecurringRowUi(
    val ruleId: String,
    val description: String,
    val categoryIcon: String,
    val categoryColorArgb: Int,
    val type: TransactionType,
    val dayOfMonth: Int,
    val defaultAmountCents: Long,
    val isPaid: Boolean,
    val paidAmountCents: Long?,
    val paidTransactionId: String?,
    val isOverdue: Boolean
)

data class RecurringUiState(
    val yearMonth: Int = 0,
    val monthLabel: String = "",
    val rows: List<RecurringRowUi> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val isLoading: Boolean = true
)

/** Lançamento do mês ainda não vinculado a nenhuma conta fixa — candidato a "já paguei" (dado importado ou manual antigo). */
data class LinkCandidateUi(
    val transactionId: String,
    val description: String,
    val amountCents: Long,
    val occurredAt: Long
)
