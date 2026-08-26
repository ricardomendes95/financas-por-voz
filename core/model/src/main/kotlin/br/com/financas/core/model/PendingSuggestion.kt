package br.com.financas.core.model

/** Sugestão pendente de uma notificação bancária — nunca vira lançamento sozinha (§8.4). */
data class PendingSuggestion(
    val id: String,
    val amountCents: Long,
    val type: TransactionType,
    val merchantRaw: String,
    val categoryId: String,
    val detectedAt: Long,
    val sourcePackage: String
)
