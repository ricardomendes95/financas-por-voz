package br.com.financas.core.data.insight

import br.com.financas.core.model.RecurringCandidate
import br.com.financas.core.model.Transaction
import kotlin.math.abs

/**
 * Algoritmo de detecção de assinaturas/recorrências (§7.1): agrupa por
 * `merchantNormalized` (ou descrição normalizada quando ausente), exige
 * pelo menos 3 ocorrências com intervalo de 28±5 dias e valor variando no
 * máximo 15% em torno da média.
 */
object RecurrenceDetector {
    private const val MIN_OCCURRENCES = 3
    private const val INTERVAL_MIN_DAYS = 23L
    private const val INTERVAL_MAX_DAYS = 33L
    private const val MAX_VARIATION = 0.15
    private const val MILLIS_PER_DAY = 86_400_000L

    fun detect(expenses: List<Transaction>): List<RecurringCandidate> =
        expenses.groupBy { it.merchantNormalized ?: normalizedKey(it.description) }
            .mapNotNull { (key, transactions) -> toCandidate(key, transactions) }

    private fun toCandidate(key: String, transactions: List<Transaction>): RecurringCandidate? {
        val sorted = transactions.sortedBy { it.occurredAt }
        if (sorted.size < MIN_OCCURRENCES) return null

        val average = sorted.map { it.amountCents }.average()
        if (average <= 0.0) return null
        val withinVariation = sorted.all { abs(it.amountCents - average) / average <= MAX_VARIATION }
        val intervalsOk = sorted.zipWithNext().all { (a, b) ->
            val days = (b.occurredAt - a.occurredAt) / MILLIS_PER_DAY
            days in INTERVAL_MIN_DAYS..INTERVAL_MAX_DAYS
        }
        if (!withinVariation || !intervalsOk) return null

        val last = sorted.last()
        val previous = sorted.getOrNull(sorted.size - 2)
        return RecurringCandidate(
            key = key,
            label = last.description,
            averageAmountCents = average.toLong(),
            occurrences = sorted.size,
            lastOccurredAt = last.occurredAt,
            lastAmountCents = last.amountCents,
            previousAmountCents = previous?.amountCents,
            categoryId = last.categoryId
        )
    }

    private fun normalizedKey(description: String): String = description.trim().lowercase()
}
