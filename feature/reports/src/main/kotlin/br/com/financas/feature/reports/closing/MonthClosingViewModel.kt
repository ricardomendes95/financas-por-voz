package br.com.financas.feature.reports.closing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.financas.core.data.export.CsvExporter
import br.com.financas.core.data.insight.InsightEngine
import br.com.financas.core.data.repository.CategoryRepository
import br.com.financas.core.data.repository.ReportsRepository
import br.com.financas.core.data.repository.TransactionRepository
import br.com.financas.core.model.Category
import br.com.financas.feature.reports.CategoryReportUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/** Resumo do mês que fechou (§7.4) — sempre o mês anterior ao atual. */
@HiltViewModel
class MonthClosingViewModel @Inject constructor(
    private val reportsRepository: ReportsRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val insightEngine: InsightEngine,
    clock: Clock
) : ViewModel() {

    private val closedYearMonth: Int = run {
        val lastMonth = YearMonth.from(LocalDate.now(clock)).minusMonths(1)
        lastMonth.year * 100 + lastMonth.monthValue
    }

    private val _uiState = MutableStateFlow(MonthClosingUiState())
    val uiState: StateFlow<MonthClosingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val categories = categoryRepository.observeActive().first().associateBy(Category::id)
            val categoryReport = reportsRepository.categoryReport(closedYearMonth)
            val totalExpense = categoryReport.sumOf { it.totalCents }.coerceAtLeast(1)
            val categoryReportUi = categoryReport.map { row ->
                val category = categories[row.categoryId]
                CategoryReportUi(
                    categoryId = row.categoryId,
                    name = category?.name ?: "Outros",
                    icon = category?.icon ?: "more_horiz",
                    colorArgb = category?.colorArgb ?: 0xFF94A3B8.toInt(),
                    totalCents = row.totalCents,
                    count = row.count,
                    percentOfTotal = row.totalCents.toFloat() / totalExpense,
                    deltaVsAveragePercent = null
                )
            }

            val trend = reportsRepository.monthlyTrend(12)
            val closedTrend = trend.firstOrNull { it.yearMonth == closedYearMonth }
            val previousIndex = trend.indexOfFirst { it.yearMonth == closedYearMonth } - 1
            val previous = trend.getOrNull(previousIndex)
            val vsLastMonth = if (previous != null && previous.expenseCents > 0 && closedTrend != null) {
                (((closedTrend.expenseCents - previous.expenseCents).toDouble() / previous.expenseCents) * 100).toInt()
            } else null

            val income = closedTrend?.incomeCents ?: 0L
            val expense = closedTrend?.expenseCents ?: 0L
            val savingsRate = if (income > 0) (((income - expense).toDouble() / income) * 100).toInt() else 0

            val insights = insightEngine.generate(closedYearMonth).filter { it.impactCents > 0 }.take(5)

            val transactions = transactionRepository.observeByMonth(closedYearMonth).first()
            val csv = CsvExporter.export(transactions, categories)

            _uiState.update {
                it.copy(
                    monthLabel = monthLabel(closedYearMonth),
                    incomeCents = income,
                    expenseCents = expense,
                    balanceCents = income - expense,
                    savingsRatePercent = savingsRate,
                    topCategories = categoryReportUi.take(3),
                    opportunities = insights,
                    vsLastMonthPercent = vsLastMonth,
                    csvContent = csv,
                    isLoading = false
                )
            }
        }
    }

    private fun monthLabel(yearMonth: Int): String {
        val month = java.time.Month.of(yearMonth % 100)
        val name = month.getDisplayName(TextStyle.FULL, Locale.Builder().setLanguage("pt").setRegion("BR").build())
        return "${name.replaceFirstChar { it.uppercase() }} ${yearMonth / 100}"
    }
}
