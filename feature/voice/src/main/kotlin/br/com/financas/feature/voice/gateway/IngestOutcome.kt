package br.com.financas.feature.voice.gateway

/** Resultado de `QuickEntryGateway.ingest` — o que cada rota mostra ao usuário. */
sealed interface IngestOutcome {
    data class Recorded(
        val transactionId: String,
        val amountCents: Long,
        val isExpense: Boolean,
        val description: String,
        val categoryName: String,
        val categoryIcon: String,
        val needsReview: Boolean
    ) : IngestOutcome

    /** Valor não encontrado ou frase vazia — nada é gravado (§4.1, etapa 8). */
    data class NotUnderstood(val rawText: String) : IngestOutcome
}
