package br.com.financas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.financas.core.database.entity.RecurringRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Schema reservado desde a v1. Usado tanto por contas fixas criadas
 * manualmente pelo usuário (`detectedAutomatically = false`) quanto,
 * futuramente, pelo `InsightEngine` (Fase 5).
 */
@Dao
interface RecurringRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurringRuleEntity)

    @Query("SELECT * FROM recurring_rules WHERE active = 1 ORDER BY dayOfMonth")
    fun observeActive(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules ORDER BY dayOfMonth")
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules WHERE id = :id")
    suspend fun getById(id: String): RecurringRuleEntity?

    @Query("UPDATE recurring_rules SET active = :active WHERE id = :id")
    suspend fun setActive(id: String, active: Boolean)

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun deleteById(id: String)
}
