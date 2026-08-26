package br.com.financas.core.data.mapper

import br.com.financas.core.database.entity.BudgetEntity
import br.com.financas.core.model.Budget

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    categoryId = categoryId,
    yearMonth = yearMonth,
    limitCents = limitCents,
    rollover = rollover
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    categoryId = categoryId,
    yearMonth = yearMonth,
    limitCents = limitCents,
    rollover = rollover
)
