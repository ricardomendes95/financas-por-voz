package br.com.financas.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.financas.core.common.YearMonthUtils
import br.com.financas.core.data.repository.CategoryRepository
import br.com.financas.core.data.repository.RecurringRuleRepository
import br.com.financas.core.data.repository.TransactionRepository
import br.com.financas.core.model.Account
import br.com.financas.core.model.Category
import br.com.financas.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRuleRepository: RecurringRuleRepository,
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    private val clock: Clock
) : ViewModel() {

    private val zone: ZoneId = clock.zone
    private val currentYearMonth = YearMonthUtils.currentYearMonth(zone)
    private val selectedYearMonth = MutableStateFlow(currentYearMonth)

    fun onPreviousMonth() {
        selectedYearMonth.update { YearMonthUtils.plusMonths(it, -1) }
    }

    fun onNextMonth() {
        selectedYearMonth.update { YearMonthUtils.plusMonths(it, 1) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<RecurringUiState> = combine(
        selectedYearMonth.flatMapLatest(recurringRuleRepository::observeActiveWithStatus),
        categoryRepository.observeActive(),
        selectedYearMonth
    ) { statuses, categories, yearMonth ->
        val categoryById = categories.associateBy(Category::id)
        val today = LocalDate.now(clock)
        val rows = statuses.map { status ->
            val category = categoryById[status.rule.categoryId]
            // "Atrasado" só faz sentido olhando o mês corrente — meses passados sem
            // pagamento continuam "Pendente" (o usuário pode estar só conferindo o histórico).
            val isOverdue = status.paidTransaction == null &&
                yearMonth == currentYearMonth &&
                status.rule.dayOfMonth < today.dayOfMonth
            RecurringRowUi(
                ruleId = status.rule.id,
                description = status.rule.description,
                categoryIcon = category?.icon ?: "more_horiz",
                categoryColorArgb = category?.colorArgb ?: 0xFF94A3B8.toInt(),
                type = status.rule.type,
                dayOfMonth = status.rule.dayOfMonth,
                defaultAmountCents = status.rule.amountCents,
                isPaid = status.paidTransaction != null,
                paidAmountCents = status.paidTransaction?.amountCents,
                paidTransactionId = status.paidTransaction?.id,
                isOverdue = isOverdue
            )
        }
        RecurringUiState(
            yearMonth = yearMonth,
            monthLabel = YearMonthUtils.fullMonthLabel(yearMonth),
            rows = rows,
            allCategories = categories,
            isLoading = false
        )
    }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringUiState())

    fun onConfirmPayment(ruleId: String, amountCents: Long) {
        viewModelScope.launch {
            recurringRuleRepository.confirmPayment(
                ruleId = ruleId,
                amountCents = amountCents,
                occurredAt = clock.millis(),
                accountId = Account.DEFAULT_ID
            )
        }
    }

    fun onCreateRule(description: String, amountCents: Long, categoryId: String, type: TransactionType, dayOfMonth: Int) {
        viewModelScope.launch {
            recurringRuleRepository.create(
                description = description,
                amountCents = amountCents,
                categoryId = categoryId,
                type = type,
                dayOfMonth = dayOfMonth
            )
        }
    }

    /** Exclusão definitiva da conta fixa — os lançamentos já criados a partir dela continuam no histórico. */
    fun onDeleteRule(ruleId: String) {
        viewModelScope.launch { recurringRuleRepository.delete(ruleId) }
    }

    private val _linkCandidates = MutableStateFlow<List<LinkCandidateUi>>(emptyList())
    val linkCandidates: StateFlow<List<LinkCandidateUi>> = _linkCandidates.asStateFlow()

    /** Lançamentos do mês selecionado que ainda não pertencem a nenhuma conta fixa — candidatos a "já paguei". */
    fun onLoadLinkCandidates(type: TransactionType) {
        viewModelScope.launch {
            val yearMonth = uiState.value.yearMonth
            _linkCandidates.value = transactionRepository.observeByMonth(yearMonth).first()
                .filter { it.type == type && it.recurrenceGroupId == null }
                .map { LinkCandidateUi(it.id, it.description, it.amountCents, it.occurredAt) }
        }
    }

    fun onClearLinkCandidates() {
        _linkCandidates.value = emptyList()
    }

    fun onLinkExistingPayment(ruleId: String, transactionId: String) {
        viewModelScope.launch {
            recurringRuleRepository.linkExistingPayment(ruleId, transactionId)
            _linkCandidates.value = emptyList()
        }
    }
}
