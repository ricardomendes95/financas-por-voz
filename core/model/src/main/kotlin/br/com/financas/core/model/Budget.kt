package br.com.financas.core.model

/** `categoryId == null` é o orçamento geral do mês. */
data class Budget(
    val id: String,
    val categoryId: String?,
    val yearMonth: Int,
    val limitCents: Long,
    val rollover: Boolean
)
