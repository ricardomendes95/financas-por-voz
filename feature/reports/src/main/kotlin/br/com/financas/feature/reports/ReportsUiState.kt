package br.com.financas.feature.reports

import br.com.financas.core.model.MonthlyTrend
import br.com.financas.core.model.OverviewStats
import br.com.financas.core.model.PaymentMethodBreakdown
import br.com.financas.core.model.WeekdayBreakdown

enum class ReportTab { OVERVIEW, CATEGORIES, EVOLUTION, COMPARE, YEARLY }

data class CategoryReportUi(
    val categoryId: String,
    val name: String,
    val icon: String,
    val colorArgb: Int,
    val totalCents: Long,
    val count: Int,
    val percentOfTotal: Float,
    val deltaVsAveragePercent: Int?
)

data class ComparisonRowUi(
    val categoryName: String,
    val monthACents: Long,
    val monthBCents: Long,
    val deltaCents: Long,
    val deltaPercent: Int?
)

data class YearComparisonRowUi(
    val categoryName: String,
    val yearACents: Long,
    val yearBCents: Long,
    val deltaCents: Long,
    val deltaPercent: Int?
)

data class ReportsUiState(
    val selectedTab: ReportTab = ReportTab.OVERVIEW,
    val yearMonth: Int = 0,
    val monthLabel: String = "",
    val overview: OverviewStats = OverviewStats(0, 0, 0, 0),
    val weekdayBreakdown: List<WeekdayBreakdown> = emptyList(),
    val paymentBreakdown: List<PaymentMethodBreakdown> = emptyList(),
    val categoryReport: List<CategoryReportUi> = emptyList(),
    val trend: List<MonthlyTrend> = emptyList(),
    val comparison: List<ComparisonRowUi> = emptyList(),
    val monthALabel: String = "",
    val monthBLabel: String = "",
    val selectedYear: Int = 0,
    val yearlyIncomeCents: Long = 0L,
    val yearlyExpenseCents: Long = 0L,
    val yearlyCategoryReport: List<CategoryReportUi> = emptyList(),
    val yearComparison: List<YearComparisonRowUi> = emptyList(),
    val yearALabel: String = "",
    val yearBLabel: String = "",
    val isLoading: Boolean = true,
    val isYearlyLoading: Boolean = true
)
