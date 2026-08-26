package br.com.financas.core.model

/** Resumo agregado de um mês — sempre calculado via `SUM`/`GROUP BY` em SQL. */
data class MonthlySummary(
    val yearMonth: Int,
    val totalIncomeCents: Long,
    val totalExpenseCents: Long
) {
    val balanceCents: Long get() = totalIncomeCents - totalExpenseCents
}
