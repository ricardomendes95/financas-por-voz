package br.com.financas.core.model

/**
 * Lançamento financeiro já persistido.
 *
 * `amountCents` é sempre `Long`, nunca `Double`/`Float` — dinheiro em
 * centavos. `occurredAt` (quando o fato aconteceu) é distinto de `createdAt`
 * (quando foi registrado); todo relatório usa `occurredAt`. Ambos em epoch
 * millis, formatação fica na camada de UI.
 */
data class Transaction(
    val id: String,
    val amountCents: Long,
    val type: TransactionType,
    val description: String,
    val rawInput: String?,
    val categoryId: String,
    val accountId: String,
    val occurredAt: Long,
    val createdAt: Long,
    val paymentMethod: PaymentMethod?,
    val source: EntrySource,
    val confidence: Float?,
    val needsReview: Boolean = false,
    val isRecurring: Boolean = false,
    val recurrenceGroupId: String? = null,
    val merchantNormalized: String? = null,
    val note: String? = null,
    val excludeFromReports: Boolean = false,
    val externalId: String? = null
)

/** Rascunho de lançamento ainda não persistido — usado pelo formulário manual. */
data class TransactionDraft(
    val amountCents: Long,
    val type: TransactionType,
    val description: String,
    val categoryId: String,
    val accountId: String,
    val occurredAt: Long,
    val paymentMethod: PaymentMethod?,
    val note: String? = null
)
