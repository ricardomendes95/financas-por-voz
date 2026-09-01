package br.com.financas.core.model

/** Resumo agregado de um mês — sempre calculado via `SUM`/`GROUP BY` em SQL. */
data class MonthlySummary(
    val yearMonth: Int,
    val totalIncomeCents: Long,
    val totalExpenseCents: Long,
    /** Saldo de todos os meses ANTERIORES a `yearMonth` — o carry-over que não deixa o saldo zerar na virada do mês. */
    val carryOverCents: Long = 0L
) {
    /** Resultado só deste mês (entrou - saiu) — usado para "onde eu desperdicei este mês". */
    val balanceCents: Long get() = totalIncomeCents - totalExpenseCents

    /** Saldo total considerando o histórico inteiro — é o que deve aparecer como saldo "de verdade" na Dashboard. */
    val accumulatedBalanceCents: Long get() = carryOverCents + balanceCents
}
