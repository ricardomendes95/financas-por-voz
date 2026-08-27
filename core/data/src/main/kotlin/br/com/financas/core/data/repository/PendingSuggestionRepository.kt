package br.com.financas.core.data.repository

import br.com.financas.core.database.dao.PendingSuggestionDao
import br.com.financas.core.database.entity.PendingSuggestionEntity
import br.com.financas.core.model.Account
import br.com.financas.core.model.EntrySource
import br.com.financas.core.model.PendingSuggestion
import br.com.financas.core.model.TransactionType
import br.com.financas.nlu.category.DefaultCategories
import br.com.financas.nlu.category.CategoryClassifier
import br.com.financas.nlu.model.CategoryRef
import br.com.financas.nlu.model.CategoryRule
import br.com.financas.nlu.model.ParseContext
import br.com.financas.nlu.model.TransactionType as NluTransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Só cria *sugestões*; nunca lança automaticamente (regra §8.4, item 1) —
 * quem confirma é sempre o usuário, na bandeja "Pendentes" do Dashboard.
 */
@Singleton
class PendingSuggestionRepository @Inject constructor(
    private val dao: PendingSuggestionDao,
    private val categoryRepository: CategoryRepository,
    private val categoryRuleRepository: CategoryRuleRepository,
    private val transactionRepository: TransactionRepository,
    private val clock: Clock
) {
    fun observePending(): Flow<List<PendingSuggestion>> =
        dao.observePending().map { list -> list.map { it.toDomain() } }

    /**
     * @param detectedAt hora real do evento (ex.: `StatusBarNotification.postTime`) — não a hora
     *   em que o app processou a notificação, que pode chegar alguns segundos depois.
     * @return `true` se uma sugestão nova foi criada; `false` se descartada
     *   por deduplicação (§8.4.3: mesmo valor ±1 centavo, ±30 min de uma
     *   transação já existente).
     */
    suspend fun suggest(
        amountCents: Long,
        type: TransactionType,
        merchantRaw: String,
        sourcePackage: String,
        detectedAt: Long = clock.millis()
    ): Boolean {
        val duplicates = dao.countPossibleDuplicates(amountCents, detectedAt - THIRTY_MIN_MS, detectedAt + THIRTY_MIN_MS)
        if (duplicates > 0) return false

        val categoryId = classifyCategory(merchantRaw, type)
        dao.insert(
            PendingSuggestionEntity(
                id = UUID.randomUUID().toString(),
                amountCents = amountCents,
                type = type,
                merchantRaw = merchantRaw,
                merchantNormalized = merchantRaw.trim().lowercase(),
                categoryId = categoryId,
                detectedAt = detectedAt,
                sourcePackage = sourcePackage,
                status = STATUS_PENDING
            )
        )
        return true
    }

    suspend fun confirm(id: String) {
        val suggestion = dao.getPending().firstOrNull { it.id == id } ?: return
        transactionRepository.createFromVoice(
            amountCents = suggestion.amountCents,
            type = suggestion.type,
            description = suggestion.merchantRaw,
            rawInput = suggestion.merchantRaw,
            categoryId = suggestion.categoryId,
            accountId = Account.DEFAULT_ID,
            occurredAt = suggestion.detectedAt,
            paymentMethod = null,
            confidence = 0.8f,
            needsReview = false,
            source = EntrySource.NOTIFICATION
        )
        dao.updateStatus(id, STATUS_CONFIRMED)
    }

    suspend fun ignore(id: String) = dao.updateStatus(id, STATUS_IGNORED)

    private suspend fun classifyCategory(merchant: String, type: TransactionType): String {
        val categories = categoryRepository.observeActive().first()
        val rules = categoryRuleRepository.observeAll().first()
        val fallback = categories.firstOrNull { it.id == DefaultCategories.Id.OTHER_EXPENSE }?.id
            ?: categories.first().id
        val fallbackIncome = categories.firstOrNull { it.id == DefaultCategories.Id.OTHER_INCOME }?.id ?: fallback

        val nluType = if (type == TransactionType.INCOME) NluTransactionType.INCOME else NluTransactionType.EXPENSE
        val context = ParseContext(
            now = LocalDateTime.now(clock.withZone(ZoneId.systemDefault())),
            categories = categories.map { CategoryRef(it.id, it.name, it.type?.let { t -> if (t == TransactionType.INCOME) NluTransactionType.INCOME else NluTransactionType.EXPENSE }) },
            rules = rules.map { CategoryRule(it.keyword, it.categoryId, it.weight, it.isUserDefined) },
            fallbackExpenseCategoryId = fallback,
            fallbackIncomeCategoryId = fallbackIncome
        )
        return CategoryClassifier.classify(merchant, nluType, context).categoryId
    }

    private fun PendingSuggestionEntity.toDomain() = PendingSuggestion(
        id = id,
        amountCents = amountCents,
        type = type,
        merchantRaw = merchantRaw,
        categoryId = categoryId,
        detectedAt = detectedAt,
        sourcePackage = sourcePackage
    )

    private companion object {
        const val THIRTY_MIN_MS = 30 * 60 * 1000L
        const val STATUS_PENDING = "PENDING"
        const val STATUS_CONFIRMED = "CONFIRMED"
        const val STATUS_IGNORED = "IGNORED"
    }
}
