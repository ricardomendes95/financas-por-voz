package br.com.financas.feature.voice.gateway

import br.com.financas.core.model.Category
import br.com.financas.core.model.CategoryRule
import br.com.financas.core.model.PaymentMethod
import br.com.financas.core.model.TransactionType
import br.com.financas.nlu.model.CategoryRef
import br.com.financas.nlu.model.ParseContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import br.com.financas.nlu.model.CategoryRule as NluCategoryRule
import br.com.financas.nlu.model.PaymentMethod as NluPaymentMethod
import br.com.financas.nlu.model.TransactionType as NluTransactionType

/**
 * Traduz entre o vocabulário do `:nlu` (parser, Kotlin puro) e o do
 * `:core:model` (domínio persistente). Os dois módulos definem os mesmos
 * conceitos de propósito — não os unificamos porque o parser não deve
 * depender da camada de domínio da app, nem vice-versa.
 */
object NluMapper {

    fun toParseContext(
        categories: List<Category>,
        rules: List<CategoryRule>,
        fallbackExpenseCategoryId: String,
        fallbackIncomeCategoryId: String,
        now: LocalDateTime
    ): ParseContext = ParseContext(
        now = now,
        categories = categories.map { CategoryRef(it.id, it.name, it.type?.toNlu()) },
        rules = rules.map { NluCategoryRule(it.keyword, it.categoryId, it.weight, it.isUserDefined) },
        fallbackExpenseCategoryId = fallbackExpenseCategoryId,
        fallbackIncomeCategoryId = fallbackIncomeCategoryId
    )

    fun TransactionType.toNlu(): NluTransactionType = when (this) {
        TransactionType.EXPENSE -> NluTransactionType.EXPENSE
        TransactionType.INCOME -> NluTransactionType.INCOME
    }

    fun NluTransactionType.toDomain(): TransactionType = when (this) {
        NluTransactionType.EXPENSE -> TransactionType.EXPENSE
        NluTransactionType.INCOME -> TransactionType.INCOME
    }

    fun NluPaymentMethod.toDomain(): PaymentMethod = when (this) {
        NluPaymentMethod.PIX -> PaymentMethod.PIX
        NluPaymentMethod.CREDIT -> PaymentMethod.CREDIT
        NluPaymentMethod.DEBIT -> PaymentMethod.DEBIT
        NluPaymentMethod.CASH -> PaymentMethod.CASH
        NluPaymentMethod.BOLETO -> PaymentMethod.BOLETO
        NluPaymentMethod.TRANSFER -> PaymentMethod.TRANSFER
    }

    fun nowAt(zone: ZoneId, epochMillis: Long): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDateTime()

    fun LocalDateTime.toEpochMillis(zone: ZoneId): Long =
        atZone(zone).toInstant().toEpochMilli()
}
