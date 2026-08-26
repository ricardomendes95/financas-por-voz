package br.com.financas.feature.transactions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.financas.core.common.MoneyFormatter
import br.com.financas.core.data.repository.AccountRepository
import br.com.financas.core.data.repository.CategoryRepository
import br.com.financas.core.data.repository.TransactionRepository
import br.com.financas.core.model.Account
import br.com.financas.core.model.EntrySource
import br.com.financas.core.model.PaymentMethod
import br.com.financas.core.model.TransactionDraft
import br.com.financas.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Presente quando a tela foi aberta para editar um lançamento existente (deep link `financas://edit`). */
    private val editingId: String? = savedStateHandle["transactionId"]

    private val form = MutableStateFlow(
        AddEditTransactionUiState(
            occurredAt = clock.millis(),
            type = savedStateHandle.get<String>("type")
                ?.let { runCatching { TransactionType.valueOf(it.uppercase()) }.getOrNull() }
                ?: TransactionType.EXPENSE,
            isEditing = editingId != null
        )
    )
    private val events = Channel<AddEditTransactionEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<AddEditTransactionUiState> = combine(
        form,
        categoryRepository.observeActive()
    ) { state, categories ->
        state.copy(allCategories = categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    init {
        editingId?.let { id ->
            viewModelScope.launch {
                transactionRepository.getById(id)?.let { transaction ->
                    form.update {
                        it.copy(
                            amountText = MoneyFormatter.formatPlain(transaction.amountCents),
                            type = transaction.type,
                            description = transaction.description,
                            selectedCategoryId = transaction.categoryId,
                            paymentMethod = transaction.paymentMethod,
                            occurredAt = transaction.occurredAt
                        )
                    }
                }
            }
        }
    }

    fun onAmountChange(text: String) {
        form.update { it.copy(amountText = text, amountInvalid = text.isNotBlank() && MoneyFormatter.parseToCents(text) == null) }
    }

    fun onTypeChange(type: TransactionType) {
        form.update { current ->
            val stillValid = current.allCategories
                .firstOrNull { it.id == current.selectedCategoryId }
                ?.let { it.type == null || it.type == type } ?: false
            current.copy(type = type, selectedCategoryId = if (stillValid) current.selectedCategoryId else null)
        }
    }

    fun onDescriptionChange(text: String) {
        form.update { it.copy(description = text) }
    }

    fun onCategorySelect(categoryId: String) {
        form.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onPaymentMethodChange(method: PaymentMethod?) {
        form.update { it.copy(paymentMethod = if (it.paymentMethod == method) null else method) }
    }

    fun onDateChange(epochMillis: Long) {
        form.update { it.copy(occurredAt = epochMillis) }
    }

    fun onSave() {
        val state = uiState.value
        val cents = MoneyFormatter.parseToCents(state.amountText) ?: return
        val categoryId = state.selectedCategoryId ?: return
        if (state.isSaving) return

        form.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val description = state.description.ifBlank { defaultDescription(state.type) }
            val currentEditingId = editingId
            if (currentEditingId != null) {
                val existing = transactionRepository.getById(currentEditingId)
                if (existing != null) {
                    transactionRepository.update(
                        existing.copy(
                            amountCents = cents,
                            type = state.type,
                            description = description,
                            categoryId = categoryId,
                            occurredAt = state.occurredAt,
                            paymentMethod = state.paymentMethod
                        )
                    )
                }
            } else {
                accountRepository.seedIfEmpty()
                transactionRepository.create(
                    draft = TransactionDraft(
                        amountCents = cents,
                        type = state.type,
                        description = description,
                        categoryId = categoryId,
                        accountId = Account.DEFAULT_ID,
                        occurredAt = state.occurredAt,
                        paymentMethod = state.paymentMethod
                    ),
                    source = EntrySource.MANUAL
                )
            }
            events.send(AddEditTransactionEvent.Saved)
        }
    }

    fun onDelete() {
        val id = editingId ?: return
        if (uiState.value.isSaving) return
        form.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            transactionRepository.delete(id)
            events.send(AddEditTransactionEvent.Deleted)
        }
    }

    private fun defaultDescription(type: TransactionType): String =
        if (type == TransactionType.EXPENSE) "Despesa" else "Receita"
}
