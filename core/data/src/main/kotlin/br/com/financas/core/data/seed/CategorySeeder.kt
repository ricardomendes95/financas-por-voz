package br.com.financas.core.data.seed

import br.com.financas.core.database.entity.CategoryEntity
import br.com.financas.core.database.entity.CategoryRuleEntity
import br.com.financas.core.model.TransactionType
import br.com.financas.nlu.category.DefaultCategories
import br.com.financas.nlu.category.DefaultCategories.Id
import br.com.financas.nlu.model.TransactionType as NluTransactionType

/**
 * Popula `categories`/`category_rules` no primeiro boot do Room a partir do
 * `:nlu`, reaproveitando `DefaultCategories.categories` (ids/nomes) e
 * `DefaultCategories.rules` (as ~250 keywords da §3.3) — para os
 * `categoryId` gravados no banco serem sempre os mesmos que o parser produz.
 * Ícone e cor não existem no `:nlu` (é dado de apresentação), então entram
 * aqui como o mapa fixo da tabela §3.3 da spec.
 */
object CategorySeeder {

    private data class Presentation(val icon: String, val colorArgb: Int, val sortOrder: Int)

    private val PRESENTATION: Map<String, Presentation> = mapOf(
        Id.FOOD to Presentation("restaurant", 0xFFF97316.toInt(), 0),
        Id.TRANSPORT to Presentation("directions_car", 0xFF3B82F6.toInt(), 1),
        Id.HOUSING to Presentation("home", 0xFF8B5CF6.toInt(), 2),
        Id.HEALTH to Presentation("favorite", 0xFFEF4444.toInt(), 3),
        Id.ENTERTAINMENT to Presentation("movie", 0xFFEC4899.toInt(), 4),
        Id.EDUCATION to Presentation("school", 0xFF14B8A6.toInt(), 5),
        Id.SHOPPING to Presentation("shopping_bag", 0xFFF59E0B.toInt(), 6),
        Id.SERVICES to Presentation("build", 0xFF64748B.toInt(), 7),
        Id.PETS to Presentation("pets", 0xFF84CC16.toInt(), 8),
        Id.TAXES to Presentation("receipt_long", 0xFF78716C.toInt(), 9),
        Id.OTHER_EXPENSE to Presentation("more_horiz", 0xFF94A3B8.toInt(), 10),
        Id.SALARY to Presentation("payments", 0xFF22C55E.toInt(), 11),
        Id.FREELANCE to Presentation("work", 0xFF10B981.toInt(), 12),
        Id.REFUND to Presentation("undo", 0xFF06B6D4.toInt(), 13),
        Id.OTHER_INCOME to Presentation("add_circle", 0xFF22D3EE.toInt(), 14)
    )

    fun categoryEntities(): List<CategoryEntity> = DefaultCategories.categories.map { ref ->
        val presentation = PRESENTATION.getValue(ref.id)
        CategoryEntity(
            id = ref.id,
            name = ref.name,
            icon = presentation.icon,
            colorArgb = presentation.colorArgb,
            type = ref.type?.toCoreModel(),
            parentId = null,
            isSystem = true,
            sortOrder = presentation.sortOrder,
            archivedAt = null
        )
    }

    fun categoryRuleEntities(): List<CategoryRuleEntity> = DefaultCategories.rules.map { rule ->
        CategoryRuleEntity(
            id = "seed_${rule.categoryId}_${rule.keyword.replace(' ', '_')}",
            keyword = rule.keyword,
            categoryId = rule.categoryId,
            weight = rule.weight,
            isUserDefined = rule.userDefined
        )
    }

    private fun NluTransactionType.toCoreModel(): TransactionType = when (this) {
        NluTransactionType.EXPENSE -> TransactionType.EXPENSE
        NluTransactionType.INCOME -> TransactionType.INCOME
    }
}
