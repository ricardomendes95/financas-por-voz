package br.com.financas.core.model

/**
 * Projeção de `Transaction` já com o nome/ícone da categoria resolvidos —
 * o que as telas de Dashboard e Lançamentos efetivamente renderizam.
 * Compartilhada entre as duas features para não duplicar o mapeamento.
 */
data class TransactionListItem(
    val id: String,
    val description: String,
    val amountCents: Long,
    val isExpense: Boolean,
    val categoryName: String,
    val categoryIcon: String,
    val occurredAt: Long
)
