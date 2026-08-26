package br.com.financas.core.data.mapper

import br.com.financas.core.database.entity.RecurringRuleEntity
import br.com.financas.core.model.RecurringRule

fun RecurringRuleEntity.toDomain(): RecurringRule = RecurringRule(
    id = id,
    description = templateDescription,
    amountCents = amountCents,
    categoryId = categoryId,
    type = type,
    dayOfMonth = dayOfMonth,
    active = active
)

fun RecurringRule.toEntity(detectedAutomatically: Boolean = false): RecurringRuleEntity = RecurringRuleEntity(
    id = id,
    templateDescription = description,
    amountCents = amountCents,
    categoryId = categoryId,
    type = type,
    dayOfMonth = dayOfMonth,
    active = active,
    detectedAutomatically = detectedAutomatically,
    confirmedByUser = true
)
