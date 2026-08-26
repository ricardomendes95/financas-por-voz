package br.com.financas.feature.transactions

import br.com.financas.core.model.TransactionListItem
import br.com.financas.core.model.TransactionType

/** `null` em [TypeFilter.type] representa "Todos" (entradas e saídas juntas). */
enum class TypeFilter(val type: TransactionType?) {
    ALL(null),
    EXPENSE(TransactionType.EXPENSE),
    INCOME(TransactionType.INCOME)
}

data class TransactionsUiState(
    val groups: List<DayGroup> = emptyList(),
    val monthLabel: String = "",
    val typeFilter: TypeFilter = TypeFilter.ALL,
    val totalCents: Long = 0L,
    val isLoading: Boolean = true
)

data class DayGroup(
    val header: String,
    val items: List<TransactionListItem>
)
