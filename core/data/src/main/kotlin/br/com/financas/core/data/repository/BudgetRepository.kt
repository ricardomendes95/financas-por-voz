package br.com.financas.core.data.repository

import br.com.financas.core.data.mapper.toDomain
import br.com.financas.core.data.mapper.toEntity
import br.com.financas.core.database.dao.BudgetDao
import br.com.financas.core.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val dao: BudgetDao
) {
    fun observeByMonth(yearMonth: Int): Flow<List<Budget>> =
        dao.observeByMonth(yearMonth).map { list -> list.map { it.toDomain() } }.distinctUntilChanged()

    /** Id determinístico por (mês, categoria) — o upsert sempre atualiza a mesma linha. */
    suspend fun setLimit(yearMonth: Int, categoryId: String?, limitCents: Long, rollover: Boolean) {
        dao.upsert(
            Budget(
                id = budgetId(yearMonth, categoryId),
                categoryId = categoryId,
                yearMonth = yearMonth,
                limitCents = limitCents,
                rollover = rollover
            ).toEntity()
        )
    }

    /** "Copiar do mês anterior" (§6.5). */
    suspend fun copyFromPreviousMonth(yearMonth: Int) {
        val previous = YearMonth.of(yearMonth / 100, yearMonth % 100).minusMonths(1)
        val previousYearMonth = previous.year * 100 + previous.monthValue
        val previousBudgets = observeByMonth(previousYearMonth).first()
        previousBudgets.forEach { budget ->
            setLimit(yearMonth, budget.categoryId, budget.limitCents, budget.rollover)
        }
    }

    private fun budgetId(yearMonth: Int, categoryId: String?): String =
        "budget_${yearMonth}_${categoryId ?: "general"}"
}
