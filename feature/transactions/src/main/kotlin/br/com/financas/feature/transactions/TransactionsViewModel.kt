package br.com.financas.feature.transactions

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
    clock: Clock,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val zone: ZoneId = clock.zone

    private val selectedYearMonth = MutableStateFlow(YearMonthUtils.currentYearMonth(zone))
    private val typeFilter = MutableStateFlow(TypeFilter.ALL)
    // Vem preenchido quando a tela é aberta a partir de um insight do Dashboard
    // ("N lançamentos sem categoria") — já chega filtrado na categoria em questão.
    private val categoryFilter = MutableStateFlow(savedStateHandle.get<String>("categoryId"))
    private val searchQuery = MutableStateFlow("")

    fun onPreviousMonth() {
        selectedYearMonth.update { YearMonthUtils.plusMonths(it, -1) }
    }

    fun onNextMonth() {
        selectedYearMonth.update { YearMonthUtils.plusMonths(it, 1) }
    }

    fun onTypeFilterChange(filter: TypeFilter) {
        typeFilter.update { filter }
        // Trocar de tipo pode deixar a categoria selecionada incompatível (ex.: uma categoria
        // de despesa filtrada enquanto se olha só receitas) — mais simples resetar do que validar.
        categoryFilter.update { null }
    }

    fun onCategoryFilterChange(categoryId: String?) {
        categoryFilter.update { current -> if (current == categoryId) null else categoryId }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.update { query }
    }

    // Combinados num único Flow porque `combine` só tem sobrecarga fixa até 5 flows.
    private val filters: Flow<Pair<TypeFilter, String?>> =
        combine(typeFilter, categoryFilter) { type, category -> type to category }

    // Com busca ativa, os lançamentos vêm de todos os meses (não só do selecionado) —
    // é assim que dá pra achar "os fies" de vários meses digitando o nome uma vez.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val transactionsSource: Flow<List<Transaction>> =
        combine(selectedYearMonth, searchQuery) { yearMonth, query -> yearMonth to query }
            .flatMapLatest { (yearMonth, query) ->
                if (query.isBlank()) transactionRepository.observeByMonth(yearMonth) else transactionRepository.search(query)
            }

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionsSource,
        categoryRepository.observeActive(),
        selectedYearMonth,
        filters,
        searchQuery
    ) { transactions, categories, yearMonth, (filter, categoryId), query ->
        val categoryById = categories.associateBy(Category::id)
        val filtered = transactions.filter { transaction ->
            (filter.type == null || transaction.type == filter.type) &&
                (categoryId == null || transaction.categoryId == categoryId)
        }
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
            searchQuery = query,
            categoryFilter = categoryId,
            allCategories = categories,
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
