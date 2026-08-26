package br.com.financas.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorArgb: Int
)

@Entity(tableName = "transaction_tags", primaryKeys = ["transactionId", "tagId"])
data class TransactionTagCrossRef(
    val transactionId: String,
    val tagId: String
)
