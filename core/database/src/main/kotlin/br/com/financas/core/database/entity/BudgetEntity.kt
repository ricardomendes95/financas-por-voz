package br.com.financas.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "budgets", indices = [Index(value = ["yearMonth", "categoryId"], unique = true)])
data class BudgetEntity(
    @PrimaryKey val id: String,
    val categoryId: String?,
    val yearMonth: Int,
    val limitCents: Long,
    val rollover: Boolean
)
