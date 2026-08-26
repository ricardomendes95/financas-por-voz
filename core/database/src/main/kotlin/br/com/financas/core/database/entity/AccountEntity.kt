package br.com.financas.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.financas.core.model.AccountKind

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: AccountKind,
    val openingBalanceCents: Long,
    val closingDay: Int?,
    val dueDay: Int?,
    val colorArgb: Int
)
