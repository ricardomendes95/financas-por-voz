package br.com.financas.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.financas.core.common.MoneyFormatter
import br.com.financas.core.common.YearMonthUtils
import br.com.financas.core.data.repository.BudgetRepository
import br.com.financas.core.data.repository.CategoryRepository
import br.com.financas.core.data.repository.ReportsRepository
import br.com.financas.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val reportsRepository: ReportsRepository,
    clock: Clock
) : ViewModel() {

    private val yearMonth = YearMonthUtils.currentYearMonth(clock.zone)
    private val _uiState = MutableStateFlow(BudgetsUiState(yearMonth = yearMonth))
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val categories = categoryRepository.observeActive().first()
                .filter { it.type == null || it.type == TransactionType.EXPENSE }
            val budgets = budgetRepository.observeByMonth(yearMonth).first().associateBy { it.categoryId }
            val report = reportsRepository.categoryReport(yearMonth).associateBy { it.categoryId }

            val rows = categories.map { category ->
                val budget = budgets[category.id]
                val row = report[category.id]
                BudgetRowUi(
                    categoryId = category.id,
                    name = category.name,
                    icon = category.icon,
                    colorArgb = category.colorArgb,
                    limitText = budget?.limitCents?.let { MoneyFormatter.formatPlain(it) } ?: "",
                    spentCents = row?.totalCents ?: 0L,
                    suggestedCents = row?.averageOfLast3MonthsCents
                )
            }

            val general = budgets[null]
            _uiState.update {
                it.copy(
                    generalLimitText = general?.limitCents?.let { cents -> MoneyFormatter.formatPlain(cents) } ?: "",
                    generalSpentCents = report.values.sumOf { row -> row.totalCents },
                    rollover = general?.rollover ?: false,
                    rows = rows,
                    isLoading = false
                )
            }
        }
    }

    fun onGeneralLimitChange(text: String) {
        _uiState.update { it.copy(generalLimitText = text) }
    }

    fun onRolloverChange(value: Boolean) {
        _uiState.update { it.copy(rollover = value) }
    }

    fun onCategoryLimitChange(categoryId: String, text: String) {
        _uiState.update { state ->
            state.copy(rows = state.rows.map { row -> if (row.categoryId == categoryId) row.copy(limitText = text) else row })
        }
    }

    fun onApplySuggestion(categoryId: String) {
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { row ->
                    if (row.categoryId == categoryId && row.suggestedCents != null) {
                        row.copy(limitText = MoneyFormatter.formatPlain(row.suggestedCents))
                    } else row
                }
            )
        }
    }

    fun onCopyFromPreviousMonth() {
        viewModelScope.launch {
            budgetRepository.copyFromPreviousMonth(yearMonth)
            load()
        }
    }

    fun onSave() {
        val state = uiState.value
        viewModelScope.launch {
            MoneyFormatter.parseToCents(state.generalLimitText)?.let { cents ->
                budgetRepository.setLimit(yearMonth, null, cents, state.rollover)
            }
            state.rows.forEach { row ->
                MoneyFormatter.parseToCents(row.limitText)?.let { cents ->
                    budgetRepository.setLimit(yearMonth, row.categoryId, cents, false)
                }
            }
            _uiState.update { it.copy(saved = true) }
        }
    }
}
