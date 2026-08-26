package br.com.financas.feature.voice.gateway

import br.com.financas.core.data.repository.AccountRepository
import br.com.financas.core.data.repository.CategoryRepository
import br.com.financas.core.data.repository.CategoryRuleRepository
import br.com.financas.core.data.repository.TransactionRepository
import br.com.financas.core.model.Account
import br.com.financas.core.model.EntrySource
import br.com.financas.feature.voice.gateway.NluMapper.toDomain
import br.com.financas.nlu.FinanceTextParser
import br.com.financas.nlu.TransactionParser
import br.com.financas.nlu.model.TransactionType as NluTransactionType
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ponto único de interpretação de texto bruto (§0.2 e regra §11 do
 * CLAUDE.md). Todas as rotas de entrada — AppFunctions, App Actions,
 * assistente do sistema, widget, tile, notificação, deep link e automação
 * externa — chamam apenas `ingest`. Nenhuma implementa parsing próprio.
 */
@Singleton
class QuickEntryGateway @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val categoryRuleRepository: CategoryRuleRepository,
    private val accountRepository: AccountRepository,
    private val clock: Clock
) {
    private val parser: TransactionParser = FinanceTextParser()
    private val zone: ZoneId get() = clock.zone

    suspend fun ingest(rawText: String, source: EntrySource): IngestOutcome {
        accountRepository.seedIfEmpty()
        categoryRepository.seedIfEmpty()

        val categories = categoryRepository.observeActive().first()
        val rules = categoryRuleRepository.observeAll().first()
        val fallbackExpense = categories.firstOrNull { it.id == "cat_outros" }?.id
            ?: categories.first().id
        val fallbackIncome = categories.firstOrNull { it.id == "cat_outras_receitas" }?.id
            ?: fallbackExpense

        val context = NluMapper.toParseContext(
            categories = categories,
            rules = rules,
            fallbackExpenseCategoryId = fallbackExpense,
            fallbackIncomeCategoryId = fallbackIncome,
            now = NluMapper.nowAt(zone, clock.millis())
        )

        val result = parser.parse(rawText, context)
        val draft = result.draft ?: return IngestOutcome.NotUnderstood(rawText)

        val transactionId = transactionRepository.createFromVoice(
            amountCents = draft.amountCents,
            type = draft.type.toDomain(),
            description = draft.description,
            rawInput = draft.rawInput,
            categoryId = draft.categoryId,
            accountId = Account.DEFAULT_ID,
            occurredAt = draft.occurredAt.toEpochMillisAt(zone),
            paymentMethod = draft.paymentMethod?.toDomain(),
            confidence = result.confidence,
            needsReview = result.needsReview,
            source = source
        )

        val category = categories.firstOrNull { it.id == draft.categoryId }
        return IngestOutcome.Recorded(
            transactionId = transactionId,
            amountCents = draft.amountCents,
            isExpense = draft.type == NluTransactionType.EXPENSE,
            description = draft.description,
            categoryName = category?.name ?: "Outros",
            categoryIcon = category?.icon ?: "more_horiz",
            needsReview = result.needsReview
        )
    }

    private fun LocalDateTime.toEpochMillisAt(zone: ZoneId): Long =
        atZone(zone).toInstant().toEpochMilli()
}
