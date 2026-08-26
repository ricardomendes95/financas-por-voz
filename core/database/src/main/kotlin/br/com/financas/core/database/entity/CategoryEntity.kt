package br.com.financas.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.financas.core.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val colorArgb: Int,
    val type: TransactionType?,
    val parentId: String?,
    val isSystem: Boolean,
    val sortOrder: Int,
    val archivedAt: Long?
)
