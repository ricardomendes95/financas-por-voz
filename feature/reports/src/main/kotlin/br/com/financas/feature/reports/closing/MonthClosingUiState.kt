package br.com.financas.feature.reports.closing

import br.com.financas.core.model.Insight
import br.com.financas.feature.reports.CategoryReportUi

data class MonthClosingUiState(
    val monthLabel: String = "",
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    val balanceCents: Long = 0,
    /** Saldo total acumulado após fechar este mês — carry-over para o mês seguinte. */
    val accumulatedBalanceCents: Long = 0,
    val savingsRatePercent: Int = 0,
    val topCategories: List<CategoryReportUi> = emptyList(),
    /** As 5 maiores oportunidades de economia — insights com maior impacto em R$. */
    val opportunities: List<Insight> = emptyList(),
    val vsLastMonthPercent: Int? = null,
    val csvContent: String = "",
    val isLoading: Boolean = true
)
