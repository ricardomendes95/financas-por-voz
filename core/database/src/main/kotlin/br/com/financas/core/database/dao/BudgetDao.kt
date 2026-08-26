package br.com.financas.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.financas.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

/** Sem UI própria ainda — schema reservado desde a v1; a tela de orçamentos é Fase 6. */
@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    fun observeByMonth(yearMonth: Int): Flow<List<BudgetEntity>>
}
