package br.com.financas.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = TransactionEntity::class)
@Entity(tableName = "transactions_fts")
data class TransactionFts(
    val description: String,
    val note: String?,
    val merchantNormalized: String?
)
