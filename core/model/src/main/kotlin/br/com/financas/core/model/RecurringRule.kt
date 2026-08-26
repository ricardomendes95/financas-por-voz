package br.com.financas.core.model

/** Molde de uma conta fixa (financiamento, água, internet etc.) criada pelo usuário. */
data class RecurringRule(
    val id: String,
    val description: String,
    val amountCents: Long,
    val categoryId: String,
    val type: TransactionType,
    val dayOfMonth: Int,
    val active: Boolean
)

/**
 * Estado de uma [RecurringRule] em um mês específico. `paidTransaction` não
 * é um campo persistido — é o lançamento (se existir) com
 * `recurrenceGroupId == rule.id` naquele `yearMonth`; `null` significa
 * pendente.
 */
data class RecurringRuleWithStatus(
    val rule: RecurringRule,
    val paidTransaction: Transaction?
)
