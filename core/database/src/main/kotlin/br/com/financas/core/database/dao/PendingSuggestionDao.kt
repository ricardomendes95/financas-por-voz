package br.com.financas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.financas.core.database.entity.PendingSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSuggestionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(suggestion: PendingSuggestionEntity)

    @Query("SELECT * FROM pending_suggestions WHERE status = 'PENDING' ORDER BY detectedAt DESC")
    fun observePending(): Flow<List<PendingSuggestionEntity>>

    @Query("SELECT * FROM pending_suggestions WHERE status = 'PENDING' ORDER BY detectedAt DESC")
    suspend fun getPending(): List<PendingSuggestionEntity>

    @Query("UPDATE pending_suggestions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    /** Deduplicação (§8.4.3): mesmo valor ±1 centavo, dentro de ±30 min. */
    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE ABS(amountCents - :amountCents) <= 1
          AND occurredAt BETWEEN :windowStart AND :windowEnd
        """
    )
    suspend fun countPossibleDuplicates(amountCents: Long, windowStart: Long, windowEnd: Long): Int
}
