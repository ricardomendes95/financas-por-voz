package br.com.financas.core.data.repository

import br.com.financas.core.common.YearMonthUtils
import br.com.financas.core.data.mapper.toDomain
import br.com.financas.core.data.mapper.toEntity
import br.com.financas.core.database.dao.TransactionDao
import br.com.financas.core.model.EntrySource
import br.com.financas.core.model.MonthlySummary
import br.com.financas.core.model.PaymentMethod
import br.com.financas.core.model.Transaction
import br.com.financas.core.model.TransactionDraft
import br.com.financas.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    private val clock: Clock
) {

    fun observeRecent(limit: Int = 8): Flow<List<Transaction>> =
        dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    fun observeByMonth(yearMonth: Int): Flow<List<Transaction>> =
        dao.observeByMonth(yearMonth).map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    /** Busca por descrição em todos os meses — usada pela busca da tela de Transações. */
    fun search(query: String): Flow<List<Transaction>> =
        dao.search(query).map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    fun observeMonthlySummary(yearMonth: Int): Flow<MonthlySummary> =
        combine(
            dao.observeMonthlySummary(yearMonth),
            dao.observeCarryOver(yearMonth)
        ) { monthRows, carryRows ->
            val income = monthRows.firstOrNull { it.type == TransactionType.INCOME.name }?.total ?: 0L
            val expense = monthRows.firstOrNull { it.type == TransactionType.EXPENSE.name }?.total ?: 0L
            val carryIncome = carryRows.firstOrNull { it.type == TransactionType.INCOME.name }?.total ?: 0L
            val carryExpense = carryRows.firstOrNull { it.type == TransactionType.EXPENSE.name }?.total ?: 0L
            MonthlySummary(yearMonth, income, expense, carryOverCents = carryIncome - carryExpense)
        }.distinctUntilChanged()

    fun observeNeedsReviewCount(): Flow<Int> = dao.observeNeedsReviewCount().distinctUntilChanged()

    fun observeTodaySpent(): Flow<Long> {
        val today = java.time.LocalDate.now(clock)
        val startOfDay = today.atStartOfDay(clock.zone).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
        return dao.observeTodaySpent(startOfDay, endOfDay).distinctUntilChanged()
    }

    /** Cria um lançamento MANUAL a partir do formulário — `occurredAt` vem do usuário, `createdAt` é agora. */
    suspend fun create(draft: TransactionDraft, source: EntrySource): String = insert(
        amountCents = draft.amountCents,
        type = draft.type,
        description = draft.description,
        rawInput = null,
        categoryId = draft.categoryId,
        accountId = draft.accountId,
        occurredAt = draft.occurredAt,
        paymentMethod = draft.paymentMethod,
        source = source,
        confidence = null,
        needsReview = false,
        note = draft.note
    )

    /** Cria um lançamento por VOZ/NOTIFICAÇÃO — carrega a frase original e a confiança do parser. */
    suspend fun createFromVoice(
        amountCents: Long,
        type: TransactionType,
        description: String,
        rawInput: String,
        categoryId: String,
        accountId: String,
        occurredAt: Long,
        paymentMethod: PaymentMethod?,
        confidence: Float,
        needsReview: Boolean,
        source: EntrySource
    ): String = insert(
        amountCents = amountCents,
        type = type,
        description = description,
        rawInput = rawInput,
        categoryId = categoryId,
        accountId = accountId,
        occurredAt = occurredAt,
        paymentMethod = paymentMethod,
        source = source,
        confidence = confidence,
        needsReview = needsReview,
        note = null
    )

    /** Cria o lançamento de pagamento de uma conta fixa (`:feature:recurring`) — liga o lançamento à regra via `recurrenceGroupId`. */
    suspend fun createRecurring(
        amountCents: Long,
        type: TransactionType,
        description: String,
        categoryId: String,
        accountId: String,
        occurredAt: Long,
        recurringRuleId: String
    ): String = insert(
        amountCents = amountCents,
        type = type,
        description = description,
        rawInput = null,
        categoryId = categoryId,
        accountId = accountId,
        occurredAt = occurredAt,
        paymentMethod = null,
        source = EntrySource.RECURRING,
        confidence = null,
        needsReview = false,
        note = null,
        isRecurring = true,
        recurrenceGroupId = recurringRuleId
    )

    private suspend fun insert(
        amountCents: Long,
        type: TransactionType,
        description: String,
        rawInput: String?,
        categoryId: String,
        accountId: String,
        occurredAt: Long,
        paymentMethod: PaymentMethod?,
        source: EntrySource,
        confidence: Float?,
        needsReview: Boolean,
        note: String?,
        isRecurring: Boolean = false,
        recurrenceGroupId: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        val transaction = Transaction(
            id = id,
            amountCents = amountCents,
            type = type,
            description = description,
            rawInput = rawInput,
            categoryId = categoryId,
            accountId = accountId,
            occurredAt = occurredAt,
            createdAt = clock.millis(),
            paymentMethod = paymentMethod,
            source = source,
            confidence = confidence,
            needsReview = needsReview,
            note = note,
            isRecurring = isRecurring,
            recurrenceGroupId = recurrenceGroupId
        )
        dao.insert(transaction.toEntity(
            yearMonth = YearMonthUtils.yearMonthOf(occurredAt),
            dayOfWeek = YearMonthUtils.dayOfWeekOf(occurredAt)
        ))
        return id
    }

    suspend fun update(transaction: Transaction) {
        dao.update(
            transaction.toEntity(
                yearMonth = YearMonthUtils.yearMonthOf(transaction.occurredAt),
                dayOfWeek = YearMonthUtils.dayOfWeekOf(transaction.occurredAt)
            )
        )
    }

    suspend fun delete(id: String) = dao.deleteById(id)

    suspend fun getById(id: String): Transaction? = dao.getById(id)?.toDomain()
}
