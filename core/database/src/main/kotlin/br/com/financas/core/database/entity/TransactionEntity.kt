package br.com.financas.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.financas.core.model.EntrySource
import br.com.financas.core.model.PaymentMethod
import br.com.financas.core.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["occurredAt"]),
        Index(value = ["yearMonth"]),
        Index(value = ["categoryId"]),
        Index(value = ["yearMonth", "type"]),
        Index(value = ["recurrenceGroupId"]),
        Index(value = ["externalId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amountCents: Long,
    val type: TransactionType,
    val description: String,
    val rawInput: String?,
    val categoryId: String,
    val accountId: String,
    val occurredAt: Long,
    val createdAt: Long,
    val yearMonth: Int,
    val dayOfWeek: Int,
    val paymentMethod: PaymentMethod?,
    val source: EntrySource,
    val confidence: Float?,
    val needsReview: Boolean = false,
    val isRecurring: Boolean = false,
    val recurrenceGroupId: String? = null,
    val merchantNormalized: String? = null,
    val note: String? = null,
    val excludeFromReports: Boolean = false,
    /** `"<fonte>:<id>"` (ex.: `"nubank_csv:6a6e0924-..."`) — chave de deduplicação de importações de extrato. */
    val externalId: String? = null
)
