package br.com.financas.core.data.repository

import br.com.financas.core.data.mapper.toDomain
import br.com.financas.core.data.mapper.toEntity
import br.com.financas.core.database.dao.RecurringRuleDao
import br.com.financas.core.database.dao.TransactionDao
import br.com.financas.core.model.RecurringRule
import br.com.financas.core.model.RecurringRuleWithStatus
import br.com.financas.core.model.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRuleRepository @Inject constructor(
    private val dao: RecurringRuleDao,
    private val transactionDao: TransactionDao,
    private val transactionRepository: TransactionRepository
) {

    /** Regras ativas casadas com o pagamento do [yearMonth] informado (`paidTransaction == null` = pendente). */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeActiveWithStatus(yearMonth: Int): Flow<List<RecurringRuleWithStatus>> =
        dao.observeActive().flatMapLatest { entities ->
            val rules = entities.map { it.toDomain() }
            val ruleIds = rules.map { it.id }
            val paymentsFlow = if (ruleIds.isEmpty()) flowOf(emptyList()) else transactionDao.observePaymentsForMonth(ruleIds, yearMonth)
            paymentsFlow.map { payments ->
                val paymentsByRule = payments.associateBy { it.recurrenceGroupId }
                rules.map { rule -> RecurringRuleWithStatus(rule, paymentsByRule[rule.id]?.toDomain()) }
            }
        }.distinctUntilChanged()

    suspend fun create(
        description: String,
        amountCents: Long,
        categoryId: String,
        type: TransactionType,
        dayOfMonth: Int
    ): String {
        val id = UUID.randomUUID().toString()
        dao.upsert(
            RecurringRule(
                id = id,
                description = description,
                amountCents = amountCents,
                categoryId = categoryId,
                type = type,
                dayOfMonth = dayOfMonth,
                active = true
            ).toEntity()
        )
        return id
    }

    suspend fun archive(id: String) = dao.setActive(id, active = false)

    /** Remove só a regra — lançamentos já criados a partir dela continuam existindo. */
    suspend fun delete(id: String) = dao.deleteById(id)

    /**
     * Confirma o pagamento do mês: cria o lançamento via [TransactionRepository.createRecurring]
     * e, se o valor confirmado for diferente do padrão da regra (parcela variável), atualiza
     * a regra para sugerir esse novo valor no mês seguinte.
     */
    suspend fun confirmPayment(ruleId: String, amountCents: Long, occurredAt: Long, accountId: String): String {
        val entity = dao.getById(ruleId) ?: error("RecurringRule $ruleId não encontrada")
        val rule = entity.toDomain()
        val transactionId = transactionRepository.createRecurring(
            amountCents = amountCents,
            type = rule.type,
            description = rule.description,
            categoryId = rule.categoryId,
            accountId = accountId,
            occurredAt = occurredAt,
            recurringRuleId = rule.id
        )
        if (amountCents != rule.amountCents) {
            dao.upsert(rule.copy(amountCents = amountCents).toEntity())
        }
        return transactionId
    }

    /**
     * Liga um lançamento JÁ EXISTENTE à regra (ex.: um pagamento importado do extrato antes
     * de a conta fixa existir no app) — evita duplicar o lançamento só para "marcar como pago".
     * Também atualiza o valor padrão da regra, igual [confirmPayment].
     */
    suspend fun linkExistingPayment(ruleId: String, transactionId: String) {
        val rule = dao.getById(ruleId)?.toDomain() ?: error("RecurringRule $ruleId não encontrada")
        val transaction = transactionRepository.getById(transactionId) ?: error("Transaction $transactionId não encontrada")
        transactionRepository.update(transaction.copy(isRecurring = true, recurrenceGroupId = ruleId))
        if (transaction.amountCents != rule.amountCents) {
            dao.upsert(rule.copy(amountCents = transaction.amountCents).toEntity())
        }
    }
}
