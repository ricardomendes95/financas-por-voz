package br.com.financas.core.data.mapper

import br.com.financas.core.database.entity.TransactionEntity
import br.com.financas.core.model.Transaction

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amountCents = amountCents,
    type = type,
    description = description,
    rawInput = rawInput,
    categoryId = categoryId,
    accountId = accountId,
    occurredAt = occurredAt,
    createdAt = createdAt,
    paymentMethod = paymentMethod,
    source = source,
    confidence = confidence,
    needsReview = needsReview,
    isRecurring = isRecurring,
    recurrenceGroupId = recurrenceGroupId,
    merchantNormalized = merchantNormalized,
    note = note,
    excludeFromReports = excludeFromReports,
    externalId = externalId
)

fun Transaction.toEntity(yearMonth: Int, dayOfWeek: Int): TransactionEntity = TransactionEntity(
    id = id,
    amountCents = amountCents,
    type = type,
    description = description,
    rawInput = rawInput,
    categoryId = categoryId,
    accountId = accountId,
    occurredAt = occurredAt,
    createdAt = createdAt,
    yearMonth = yearMonth,
    dayOfWeek = dayOfWeek,
    paymentMethod = paymentMethod,
    source = source,
    confidence = confidence,
    needsReview = needsReview,
    isRecurring = isRecurring,
    recurrenceGroupId = recurrenceGroupId,
    merchantNormalized = merchantNormalized,
    note = note,
    excludeFromReports = excludeFromReports,
    externalId = externalId
)
