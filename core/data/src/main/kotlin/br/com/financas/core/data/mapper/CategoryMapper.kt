package br.com.financas.core.data.mapper

import br.com.financas.core.database.entity.CategoryEntity
import br.com.financas.core.database.entity.CategoryRuleEntity
import br.com.financas.core.model.Category
import br.com.financas.core.model.CategoryRule

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    colorArgb = colorArgb,
    type = type,
    parentId = parentId,
    isSystem = isSystem,
    sortOrder = sortOrder,
    archivedAt = archivedAt
)

fun CategoryRuleEntity.toDomain(): CategoryRule = CategoryRule(
    id = id,
    keyword = keyword,
    categoryId = categoryId,
    weight = weight,
    isUserDefined = isUserDefined,
    hitCount = hitCount,
    lastUsedAt = lastUsedAt
)
