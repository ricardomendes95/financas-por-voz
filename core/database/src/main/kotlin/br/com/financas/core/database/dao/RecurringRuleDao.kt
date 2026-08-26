package br.com.financas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.financas.core.database.entity.RecurringRuleEntity
import kotlinx.coroutines.flow.Flow

/** Sem UI própria ainda — schema reservado desde a v1; o `InsightEngine` popula isso na Fase 5. */
@Dao
interface RecurringRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurringRuleEntity)

    @Query("SELECT * FROM recurring_rules WHERE active = 1")
    fun observeActive(): Flow<List<RecurringRuleEntity>>
}
