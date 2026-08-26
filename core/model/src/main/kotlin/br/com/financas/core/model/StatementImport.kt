package br.com.financas.core.model

/** Uma linha já parseada de um extrato bancário — antes de qualquer dedup/categorização. */
data class StatementEntry(
    val externalId: String,
    val occurredAt: Long,
    val amountCents: Long,
    val type: TransactionType,
    val description: String
)

/** Uma linha de extrato pronta pra revisão do usuário antes de confirmar a importação. */
data class StatementPreviewItem(
    val entry: StatementEntry,
    val categoryId: String,
    val categoryName: String,
    val alreadyImported: Boolean
)
