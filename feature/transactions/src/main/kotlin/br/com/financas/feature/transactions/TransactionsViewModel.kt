package br.com.financas.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.financas.core.common.RelativeDateFormatter
import br.com.financas.core.common.YearMonthUtils
import br.com.financas.core.data.repository.CategoryRepository
import br.com.financas.core.data.repository.TransactionRepository
import br.com.financas.core.model.Category
import br.com.financas.core.model.Transaction
import br.com.financas.core.model.TransactionListItem
import br.com.financas.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    clock: Clock
) : ViewModel() {

    private val zone: ZoneId = clock.zone

    private val selectedYearMonth = MutableStateFlow(YearMonthUtils.currentYearMonth(zone))
    private val typeFilter = MutableStateFlow(TypeFilter.ALL)

    fun onPreviousMonth() {
        selectedYearMonth.update { YearMonthUtils.plusMonths(it, -1) }
    }

    fun onNextMonth() {
        selectedYearMonth.update { YearMonthUtils.plusMonths(it, 1) }
    }

    fun onTypeFilterChange(filter: TypeFilter) {
        typeFilter.update { filter }
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        selectedYearMonth.flatMapLatest(transactionRepository::observeByMonth),
        categoryRepository.observeActive(),
        selectedYearMonth,
        typeFilter
    ) { transactions, categories, yearMonth, filter ->
        val categoryById = categories.associateBy(Category::id)
        val filtered = transactions.filter { filter.type == null || it.type == filter.type }
        val items = filtered.map { it.toListItem(categoryById) }
        val groups = items
            .groupBy { RelativeDateFormatter.sectionHeader(it.occurredAt, zone) }
            .map { (header, groupItems) -> DayGroup(header, groupItems) }
        val total = filtered.sumOf { if (it.type == TransactionType.EXPENSE) -it.amountCents else it.amountCents }
        TransactionsUiState(
            groups = groups,
            monthLabel = YearMonthUtils.fullMonthLabel(yearMonth),
            typeFilter = filter,
            totalCents = total,
            isLoading = false
        )
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

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
}
