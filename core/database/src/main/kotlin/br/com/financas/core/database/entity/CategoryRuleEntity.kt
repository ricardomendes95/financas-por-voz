package br.com.financas.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "category_rules", indices = [Index("keyword", unique = true)])
data class CategoryRuleEntity(
    @PrimaryKey val id: String,
    val keyword: String,
    val categoryId: String,
    val weight: Int,
    val isUserDefined: Boolean,
    val hitCount: Int = 0,
    val lastUsedAt: Long? = null
)
