package br.com.financas.core.model

data class CategoryReportRow(
    val categoryId: String,
    val totalCents: Long,
    val count: Int,
    val averageOfLast3MonthsCents: Long?
)

data class MonthlyTrend(
    val yearMonth: Int,
    val incomeCents: Long,
    val expenseCents: Long
)

data class PaymentMethodBreakdown(
    val method: PaymentMethod?,
    val totalCents: Long,
    val count: Int
)

data class WeekdayBreakdown(
    val dayOfWeek: Int,
    val totalCents: Long
)

data class OverviewStats(
    val maxExpenseCents: Long,
    val averageDailyCents: Long,
    val noSpendDays: Int,
    val projectedMonthEndCents: Long
)

data class MonthComparisonRow(
    val categoryId: String,
    val monthACents: Long,
    val monthBCents: Long
) {
    val deltaCents: Long get() = monthBCents - monthACents
    val deltaPercent: Double? get() = if (monthACents == 0L) null else (deltaCents.toDouble() / monthACents) * 100
}

data class YearlySummary(
    val year: Int,
    val incomeCents: Long,
    val expenseCents: Long
)

data class YearComparisonRow(
    val categoryId: String,
    val yearACents: Long,
    val yearBCents: Long
) {
    val deltaCents: Long get() = yearBCents - yearACents
    val deltaPercent: Double? get() = if (yearACents == 0L) null else (deltaCents.toDouble() / yearACents) * 100
}
