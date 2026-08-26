package br.com.financas.core.data.repository

import br.com.financas.core.database.dao.TransactionDao
import br.com.financas.core.model.CategoryReportRow
import br.com.financas.core.model.MonthComparisonRow
import br.com.financas.core.model.MonthlyTrend
import br.com.financas.core.model.OverviewStats
import br.com.financas.core.model.PaymentMethod
import br.com.financas.core.model.PaymentMethodBreakdown
import br.com.financas.core.model.WeekdayBreakdown
import br.com.financas.core.model.YearComparisonRow
import br.com.financas.core.model.YearlySummary
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Todas as agregações de relatório vêm de queries `SUM`/`GROUP BY` do
 * `TransactionDao` — nenhuma soma acontece em Kotlin (regra §11). Este
 * repositório só traduz as linhas cruas para os modelos de domínio.
 */
@Singleton
class ReportsRepository @Inject constructor(
    private val dao: TransactionDao,
    private val clock: Clock
) {

    suspend fun categoryReport(yearMonth: Int): List<CategoryReportRow> {
        val current = dao.categoryTotalsForMonths(listOf(yearMonth))
        val historical = dao.categoryTotalsForMonths(lastNMonths(yearMonth, 3))
            .groupBy { it.categoryId }
            .mapValues { (_, rows) -> rows.sumOf { it.total } / 3 }

        return current
            .sortedByDescending { it.total }
            .map { row ->
                CategoryReportRow(
                    categoryId = row.categoryId,
                    totalCents = row.total,
                    count = row.qty,
                    averageOfLast3MonthsCents = historical[row.categoryId]
                )
            }
    }

    suspend fun monthlyTrend(referenceYearMonth: Int = currentYearMonth(), monthsBack: Int = 12): List<MonthlyTrend> {
        val months = (0 until monthsBack).map { offsetMonth(referenceYearMonth, -it) }.sorted()
        val rows = dao.monthlyTotalsForMonths(months)
        val byMonth = rows.groupBy { it.yearMonth }
        return months.map { ym ->
            val monthRows = byMonth[ym].orEmpty()
            MonthlyTrend(
                yearMonth = ym,
                incomeCents = monthRows.firstOrNull { it.type == "INCOME" }?.total ?: 0L,
                expenseCents = monthRows.firstOrNull { it.type == "EXPENSE" }?.total ?: 0L
            )
        }
    }

    suspend fun paymentMethodBreakdown(yearMonth: Int): List<PaymentMethodBreakdown> =
        dao.paymentMethodTotals(yearMonth).map { row ->
            PaymentMethodBreakdown(
                method = row.paymentMethod?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() },
                totalCents = row.total,
                count = row.qty
            )
        }

    suspend fun weekdayBreakdown(yearMonth: Int): List<WeekdayBreakdown> =
        dao.weekdayTotals(yearMonth).map { WeekdayBreakdown(it.dayOfWeek, it.total) }

    suspend fun overviewStats(yearMonth: Int): OverviewStats {
        val maxExpense = dao.maxExpense(yearMonth) ?: 0L
        val spendDays = dao.distinctSpendDaysInMonth(yearMonth)
        val today = LocalDate.now(clock)
        val ym = YearMonth.of(yearMonth / 100, yearMonth % 100)
        val isCurrentMonth = ym == YearMonth.from(today)
        val daysElapsed = if (isCurrentMonth) today.dayOfMonth else ym.lengthOfMonth()
        val totalExpense = dao.categoryTotalsForMonths(listOf(yearMonth)).sumOf { it.total }
        val averageDaily = if (daysElapsed > 0) totalExpense / daysElapsed else 0L
        val projected = if (isCurrentMonth) averageDaily * ym.lengthOfMonth() else totalExpense

        return OverviewStats(
            maxExpenseCents = maxExpense,
            averageDailyCents = averageDaily,
            noSpendDays = (daysElapsed - spendDays).coerceAtLeast(0),
            projectedMonthEndCents = projected
        )
    }

    suspend fun compareMonths(monthA: Int, monthB: Int): List<MonthComparisonRow> {
        val rows = dao.categoryTotalsByMonth(listOf(monthA, monthB))
        val categoryIds = rows.map { it.categoryId }.toSet()
        return categoryIds.map { categoryId ->
            MonthComparisonRow(
                categoryId = categoryId,
                monthACents = rows.firstOrNull { it.yearMonth == monthA && it.categoryId == categoryId }?.total ?: 0L,
                monthBCents = rows.firstOrNull { it.yearMonth == monthB && it.categoryId == categoryId }?.total ?: 0L
            )
        }.sortedByDescending { kotlin.math.abs(it.deltaCents) }
    }

    /** Totais de entrada/saída do ano — soma feita em SQL sobre os 12 meses (regra §11). */
    suspend fun yearlySummary(year: Int): YearlySummary {
        val rows = dao.yearlyTotalsForYears(listOf(year))
        return YearlySummary(
            year = year,
            incomeCents = rows.firstOrNull { it.type == "INCOME" }?.total ?: 0L,
            expenseCents = rows.firstOrNull { it.type == "EXPENSE" }?.total ?: 0L
        )
    }

    /** Breakdown de despesas por categoria no ano. */
    suspend fun yearlyCategoryReport(year: Int): List<CategoryReportRow> =
        dao.categoryTotalsByYear(listOf(year))
            .sortedByDescending { it.total }
            .map { row ->
                CategoryReportRow(
                    categoryId = row.categoryId,
                    totalCents = row.total,
                    count = row.qty,
                    averageOfLast3MonthsCents = null
                )
            }

    suspend fun compareYears(yearA: Int, yearB: Int): List<YearComparisonRow> {
        val rows = dao.categoryTotalsByYear(listOf(yearA, yearB))
        val categoryIds = rows.map { it.categoryId }.toSet()
        return categoryIds.map { categoryId ->
            YearComparisonRow(
                categoryId = categoryId,
                yearACents = rows.firstOrNull { it.year == yearA && it.categoryId == categoryId }?.total ?: 0L,
                yearBCents = rows.firstOrNull { it.year == yearB && it.categoryId == categoryId }?.total ?: 0L
            )
        }.sortedByDescending { kotlin.math.abs(it.deltaCents) }
    }

    private fun currentYearMonth(): Int {
        val today = LocalDate.now(clock)
        return today.year * 100 + today.monthValue
    }

    private fun offsetMonth(yearMonth: Int, offset: Int): Int {
        val ym = YearMonth.of(yearMonth / 100, yearMonth % 100).plusMonths(offset.toLong())
        return ym.year * 100 + ym.monthValue
    }

    private fun lastNMonths(yearMonth: Int, n: Int): List<Int> {
        var ym = YearMonth.of(yearMonth / 100, yearMonth % 100)
        return (1..n).map {
            ym = ym.minusMonths(1)
            ym.year * 100 + ym.monthValue
        }
    }
}
