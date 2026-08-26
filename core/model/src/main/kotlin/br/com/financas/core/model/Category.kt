package br.com.financas.core.model

/** Categoria de lançamento — despesa, receita, ou ambas quando `type == null`. */
data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val colorArgb: Int,
    val type: TransactionType?,
    val parentId: String? = null,
    val isSystem: Boolean = false,
    val sortOrder: Int = 0,
    val archivedAt: Long? = null
)

/** Regra palavra-chave → categoria, usada pelo classificador de categoria. */
data class CategoryRule(
    val id: String,
    val keyword: String,
    val categoryId: String,
    val weight: Int,
    val isUserDefined: Boolean,
    val hitCount: Int = 0,
    val lastUsedAt: Long? = null
)
