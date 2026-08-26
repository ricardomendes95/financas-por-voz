package br.com.financas.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.financas.core.common.YearMonthUtils
import br.com.financas.core.data.repository.CategoryRepository
import br.com.financas.core.data.repository.ReportsRepository
import br.com.financas.core.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportsRepository: ReportsRepository,
    private val categoryRepository: CategoryRepository,
    clock: Clock
) : ViewModel() {

    private val currentYear = LocalDate.now(clock).year

    private val _uiState = MutableStateFlow(
        ReportsUiState(yearMonth = YearMonthUtils.currentYearMonth(clock.zone), selectedYear = currentYear)
    )
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadMonthly(_uiState.value.yearMonth)
        loadYearly(currentYear)
    }

    fun onTabSelected(tab: ReportTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onPreviousMonth() {
        loadMonthly(YearMonthUtils.plusMonths(_uiState.value.yearMonth, -1))
    }

    fun onNextMonth() {
        loadMonthly(YearMonthUtils.plusMonths(_uiState.value.yearMonth, 1))
    }

    fun onPreviousYear() {
        loadYearly(_uiState.value.selectedYear - 1)
    }

    fun onNextYear() {
        loadYearly(_uiState.value.selectedYear + 1)
    }

    private fun loadMonthly(yearMonth: Int) {
        _uiState.update { it.copy(yearMonth = yearMonth, isLoading = true) }
        viewModelScope.launch {
            val categories = categoryRepository.observeActive().first().associateBy(Category::id)

            val categoryReport = reportsRepository.categoryReport(yearMonth)
            val totalExpense = categoryReport.sumOf { it.totalCents }.coerceAtLeast(1)
            val categoryReportUi = categoryReport.map { row ->
                val category = categories[row.categoryId]
                val delta = row.averageOfLast3MonthsCents?.takeIf { it > 0 }?.let {
                    (((row.totalCents - it).toDouble() / it) * 100).toInt()
                }
                CategoryReportUi(
                    categoryId = row.categoryId,
                    name = category?.name ?: "Outros",
                    icon = category?.icon ?: "more_horiz",
                    colorArgb = category?.colorArgb ?: 0xFF94A3B8.toInt(),
                    totalCents = row.totalCents,
                    count = row.count,
                    percentOfTotal = row.totalCents.toFloat() / totalExpense,
                    deltaVsAveragePercent = delta
                )
            }

            val previousYearMonth = YearMonthUtils.plusMonths(yearMonth, -1)
            val comparison = reportsRepository.compareMonths(previousYearMonth, yearMonth).map { row ->
                val category = categories[row.categoryId]
                ComparisonRowUi(
                    categoryName = category?.name ?: "Outros",
                    monthACents = row.monthACents,
                    monthBCents = row.monthBCents,
                    deltaCents = row.deltaCents,
                    deltaPercent = row.deltaPercent?.toInt()
                )
            }

            _uiState.update {
                it.copy(
                    yearMonth = yearMonth,
                    monthLabel = YearMonthUtils.fullMonthLabel(yearMonth),
                    overview = reportsRepository.overviewStats(yearMonth),
                    weekdayBreakdown = reportsRepository.weekdayBreakdown(yearMonth),
                    paymentBreakdown = reportsRepository.paymentMethodBreakdown(yearMonth),
                    categoryReport = categoryReportUi,
                    trend = reportsRepository.monthlyTrend(referenceYearMonth = yearMonth),
                    comparison = comparison,
                    monthALabel = monthLabel(previousYearMonth),
                    monthBLabel = monthLabel(yearMonth),
                    isLoading = false
                )
            }
        }
    }

    private fun loadYearly(year: Int) {
        _uiState.update { it.copy(selectedYear = year, isYearlyLoading = true) }
        viewModelScope.launch {
            val categories = categoryRepository.observeActive().first().associateBy(Category::id)

            val yearlyCategoryReport = reportsRepository.yearlyCategoryReport(year)
            val totalExpense = yearlyCategoryReport.sumOf { it.totalCents }.coerceAtLeast(1)
            val yearlyCategoryReportUi = yearlyCategoryReport.map { row ->
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

            val previousYear = year - 1
            val yearComparison = reportsRepository.compareYears(previousYear, year).map { row ->
                val category = categories[row.categoryId]
                YearComparisonRowUi(
                    categoryName = category?.name ?: "Outros",
                    yearACents = row.yearACents,
                    yearBCents = row.yearBCents,
                    deltaCents = row.deltaCents,
                    deltaPercent = row.deltaPercent?.toInt()
                )
            }

            val summary = reportsRepository.yearlySummary(year)

            _uiState.update {
                it.copy(
                    selectedYear = year,
                    yearlyIncomeCents = summary.incomeCents,
                    yearlyExpenseCents = summary.expenseCents,
                    yearlyCategoryReport = yearlyCategoryReportUi,
                    yearComparison = yearComparison,
                    yearALabel = previousYear.toString(),
                    yearBLabel = year.toString(),
                    isYearlyLoading = false
                )
            }
        }
    }

    private fun monthLabel(yearMonth: Int): String = YearMonthUtils.shortMonthLabel(yearMonth)
}
