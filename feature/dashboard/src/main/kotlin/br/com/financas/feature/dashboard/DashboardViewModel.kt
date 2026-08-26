package br.com.financas.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.financas.core.common.YearMonthUtils
import br.com.financas.core.data.insight.InsightEngine
import br.com.financas.core.data.repository.BudgetRepository
import br.com.financas.core.data.repository.CategoryRepository
import br.com.financas.core.data.repository.PendingSuggestionRepository
import br.com.financas.core.data.repository.TransactionRepository
import br.com.financas.core.model.Budget
import br.com.financas.core.model.Category
import br.com.financas.core.model.Insight
import br.com.financas.core.model.MonthlySummary
import br.com.financas.core.model.PendingSuggestion
import br.com.financas.core.model.Transaction
import br.com.financas.core.model.TransactionListItem
import br.com.financas.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    budgetRepository: BudgetRepository,
    private val pendingSuggestionRepository: PendingSuggestionRepository,
    insightEngine: InsightEngine,
    private val clock: Clock
) : ViewModel() {

    private val yearMonth = YearMonthUtils.yearMonthOf(clock.millis())

    /** Recalculado ao abrir o Dashboard (§7), não em segundo plano contínuo. */
    private val insights = MutableStateFlow<List<Insight>>(emptyList())

    init {
        viewModelScope.launch {
            insights.value = insightEngine.generate(yearMonth)
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.observeMonthlySummary(yearMonth),
        transactionRepository.observeRecent(limit = 8),
        categoryRepository.observeActive(),
        budgetRepository.observeByMonth(yearMonth),
        pendingSuggestionRepository.observePending(),
        insights
    ) { values ->
        val summary = values[0] as MonthlySummary
        @Suppress("UNCHECKED_CAST")
        val recent = values[1] as List<Transaction>
        @Suppress("UNCHECKED_CAST")
        val categories = values[2] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val budgets = values[3] as List<Budget>
        @Suppress("UNCHECKED_CAST")
        val pending = values[4] as List<PendingSuggestion>
        @Suppress("UNCHECKED_CAST")
        val insightList = values[5] as List<Insight>

        val categoryById = categories.associateBy(Category::id)
        val generalLimit = budgets.firstOrNull { it.categoryId == null }?.limitCents
            ?: budgets.sumOf { it.limitCents }.takeIf { it > 0 }

        DashboardUiState(
            yearMonth = yearMonth,
            monthLabel = YearMonthUtils.fullMonthLabel(yearMonth),
            balanceCents = summary.balanceCents,
            incomeCents = summary.totalIncomeCents,
            expenseCents = summary.totalExpenseCents,
            recentTransactions = recent.map { it.toListItem(categoryById) },
            insights = insightList.take(3),
            budgetLimitCents = generalLimit,
            budgetExpectedProgress = expectedMonthProgress(),
            pendingSuggestions = pending.map { it.toUi(categoryById) },
            isLoading = false
        )
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun onConfirmSuggestion(id: String) {
        viewModelScope.launch { pendingSuggestionRepository.confirm(id) }
    }

    fun onIgnoreSuggestion(id: String) {
        viewModelScope.launch { pendingSuggestionRepository.ignore(id) }
    }

    private fun expectedMonthProgress(): Float {
        val today = LocalDate.now(clock)
        val daysInMonth = YearMonth.from(today).lengthOfMonth()
        return today.dayOfMonth.toFloat() / daysInMonth
    }

    private fun Transaction.toListItem(categoryById: Map<String, Category>): TransactionListItem {
        val category = categoryById[categoryId]
        return TransactionListItem(
            id = id,
            description = description,
            amountCents = amountCents,
            isExpense = type == TransactionType.EXPENSE,
            categoryName = category?.name ?: "Outros",
            categoryIcon = category?.icon ?: "more_horiz",
            occurredAt = occurredAt
        )
    }

    private fun PendingSuggestion.toUi(categoryById: Map<String, Category>): PendingSuggestionUi {
        val category = categoryById[categoryId]
        return PendingSuggestionUi(
            id = id,
            amountCents = amountCents,
            isExpense = type == TransactionType.EXPENSE,
            merchant = merchantRaw,
            categoryName = category?.name ?: "Outros"
        )
    }
}
