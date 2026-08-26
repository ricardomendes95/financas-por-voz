package br.com.financas.core.model

/** Os 12 tipos de card acionável do Dashboard (§7.3). */
enum class InsightType {
    CATEGORY_SPIKE, PACE_WARNING, SUBSCRIPTION_TOTAL, ZOMBIE_SUB, MICRO_SPEND,
    EXPENSIVE_DAY, TOP_MERCHANT, NO_SPEND_STREAK, PRICE_CREEP, UNCATEGORIZED,
    SAVINGS_RATE, WEEKEND_RATIO
}

/**
 * Card de insight já pronto para exibição. Ranqueado por `impactCents`
 * (impacto em R$) — quanto maior, mais relevante.
 */
data class Insight(
    val type: InsightType,
    val message: String,
    val impactCents: Long,
    val actionLabel: String? = null,
    val relatedCategoryId: String? = null
)

/** Recorrência candidata a assinatura, detectada por `RecurrenceDetector`. */
data class RecurringCandidate(
    val key: String,
    val label: String,
    val averageAmountCents: Long,
    val occurrences: Int,
    val lastOccurredAt: Long,
    val lastAmountCents: Long,
    val previousAmountCents: Long?,
    val categoryId: String
)
