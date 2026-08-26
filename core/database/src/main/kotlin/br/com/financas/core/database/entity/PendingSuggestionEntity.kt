package br.com.financas.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.financas.core.model.TransactionType

/**
 * Sugestão pendente vinda de uma notificação bancária (§8). Nunca vira
 * lançamento sozinha — precisa de confirmação explícita na bandeja
 * "Pendentes" do Dashboard (regra §8.4, item 1).
 */
@Entity(
    tableName = "pending_suggestions",
    indices = [Index("status"), Index("detectedAt")]
)
data class PendingSuggestionEntity(
    @PrimaryKey val id: String,
    val amountCents: Long,
    val type: TransactionType,
    val merchantRaw: String,
    val merchantNormalized: String,
    val categoryId: String,
    val detectedAt: Long,
    val sourcePackage: String,
    val status: String // PENDING | CONFIRMED | IGNORED
)
