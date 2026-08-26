package br.com.financas.core.data.repository

import br.com.financas.core.common.YearMonthUtils
import br.com.financas.core.data.mapper.toEntity
import br.com.financas.core.database.dao.TransactionDao
import br.com.financas.core.model.Account
import br.com.financas.core.model.EntrySource
import br.com.financas.core.model.StatementEntry
import br.com.financas.core.model.StatementPreviewItem
import br.com.financas.core.model.Transaction
import br.com.financas.core.model.TransactionType
import br.com.financas.nlu.category.CategoryClassifier
import br.com.financas.nlu.category.DefaultCategories
import br.com.financas.nlu.model.CategoryRef
import br.com.financas.nlu.model.CategoryRule
import br.com.financas.nlu.model.ParseContext
import br.com.financas.nlu.model.TransactionType as NluTransactionType
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prepara o preview de uma importação de extrato (§5.9/§12: preview antes de
 * confirmar, dedup) e, na confirmação, grava só as linhas selecionadas que
 * ainda não existem — a categorização automática é só uma sugestão inicial,
 * o usuário revisa cada linha antes de importar.
 */
@Singleton
class StatementImportRepository @Inject constructor(
    private val dao: TransactionDao,
    private val categoryRepository: CategoryRepository,
    private val categoryRuleRepository: CategoryRuleRepository,
    private val clock: Clock
) {

    suspend fun preview(entries: List<StatementEntry>): List<StatementPreviewItem> {
        if (entries.isEmpty()) return emptyList()

        val categories = categoryRepository.observeActive().first()
        val rules = categoryRuleRepository.observeAll().first()
        val fallbackExpense = categories.firstOrNull { it.id == DefaultCategories.Id.OTHER_EXPENSE }?.id
            ?: categories.first().id
        val fallbackIncome = categories.firstOrNull { it.id == DefaultCategories.Id.OTHER_INCOME }?.id ?: fallbackExpense
        val categoriesById = categories.associateBy { it.id }

        val nluContext = ParseContext(
            now = LocalDateTime.now(clock.withZone(ZoneId.systemDefault())),
            categories = categories.map {
                CategoryRef(it.id, it.name, it.type?.let { t -> t.toNlu() })
            },
            rules = rules.map { CategoryRule(it.keyword, it.categoryId, it.weight, it.isUserDefined) },
            fallbackExpenseCategoryId = fallbackExpense,
            fallbackIncomeCategoryId = fallbackIncome
        )

        val existingIds = dao.existingExternalIds(entries.map { it.externalId }).toSet()

        return entries.map { entry ->
            val categoryId = CategoryClassifier.classify(entry.description, entry.type.toNlu(), nluContext).categoryId
            StatementPreviewItem(
                entry = entry,
                categoryId = categoryId,
                categoryName = categoriesById[categoryId]?.name ?: "Outros",
                alreadyImported = entry.externalId in existingIds
            )
        }
    }

    /** Grava as linhas selecionadas (a chamadora já filtrou duplicatas e desmarcadas). @return quantas foram gravadas. */
    suspend fun confirmImport(items: List<StatementPreviewItem>): Int {
        if (items.isEmpty()) return 0
        val now = clock.millis()
        val entities = items.map { item ->
            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                amountCents = item.entry.amountCents,
                type = item.entry.type,
                description = item.entry.description,
                rawInput = item.entry.description,
                categoryId = item.categoryId,
                accountId = Account.DEFAULT_ID,
                occurredAt = item.entry.occurredAt,
                createdAt = now,
                paymentMethod = null,
                source = EntrySource.NOTIFICATION,
                confidence = 0.7f,
                needsReview = false,
                externalId = item.entry.externalId
            )
            transaction.toEntity(
                yearMonth = YearMonthUtils.yearMonthOf(item.entry.occurredAt),
                dayOfWeek = java.time.Instant.ofEpochMilli(item.entry.occurredAt).atZone(ZoneId.systemDefault()).dayOfWeek.value
            )
        }
        dao.insertAll(entities)
        return entities.size
    }

    private fun TransactionType.toNlu(): NluTransactionType =
        if (this == TransactionType.INCOME) NluTransactionType.INCOME else NluTransactionType.EXPENSE
}
