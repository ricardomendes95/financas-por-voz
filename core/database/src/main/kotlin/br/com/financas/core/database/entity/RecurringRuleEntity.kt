package br.com.financas.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.financas.core.model.TransactionType

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    val templateDescription: String,
    val amountCents: Long,
    val categoryId: String,
    val type: TransactionType,
    val dayOfMonth: Int,
    val active: Boolean,
    val detectedAutomatically: Boolean,
    val confirmedByUser: Boolean
)
