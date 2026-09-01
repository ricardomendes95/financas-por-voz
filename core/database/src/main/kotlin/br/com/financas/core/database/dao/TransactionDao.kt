package br.com.financas.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.financas.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/** Linha crua do agregado mensal — soma feita em SQL, nunca em Kotlin (regra §11). */
data class MonthlySummaryRow(
    val type: String,
    val total: Long
)

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    /** IDs externos já importados dentre os candidatos — base da dedup de extratos (§5.9/§12). */
    @Query("SELECT externalId FROM transactions WHERE externalId IN (:externalIds)")
    suspend fun existingExternalIds(externalIds: List<String>): List<String>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun pagingSource(): PagingSource<Int, TransactionEntity>

    @Query("SELECT * FROM transactions WHERE yearMonth = :yearMonth ORDER BY occurredAt DESC")
    fun observeByMonth(yearMonth: Int): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT type, SUM(amountCents) AS total FROM transactions
        WHERE yearMonth = :yearMonth AND excludeFromReports = 0
        GROUP BY type
        """
    )
    fun observeMonthlySummary(yearMonth: Int): Flow<List<MonthlySummaryRow>>

    /**
     * Saldo acumulado de TODOS os meses anteriores ao informado — carry-over
     * entre meses (o saldo não zera na virada do mês). `yearMonth` é indexado,
     * então o `<` faz um range scan, não um `SCAN TABLE` (regra §11.10).
     */
    @Query(
        """
        SELECT type, SUM(amountCents) AS total FROM transactions
        WHERE yearMonth < :yearMonth AND excludeFromReports = 0
        GROUP BY type
        """
    )
    fun observeCarryOver(yearMonth: Int): Flow<List<MonthlySummaryRow>>

    @Query(
        """
        SELECT type, SUM(amountCents) AS total FROM transactions
        WHERE yearMonth < :yearMonth AND excludeFromReports = 0
        GROUP BY type
        """
    )
    suspend fun carryOverTotals(yearMonth: Int): List<MonthlySummaryRow>

    @Query(
        """
        SELECT categoryId, SUM(amountCents) AS total, COUNT(*) AS qty
        FROM transactions
        WHERE yearMonth = :yearMonth AND type = 'EXPENSE' AND excludeFromReports = 0
        GROUP BY categoryId ORDER BY total DESC
        """
    )
    fun observeCategoryTotals(yearMonth: Int): Flow<List<CategoryTotalRow>>

    @Query("SELECT COUNT(*) FROM transactions WHERE needsReview = 1")
    fun observeNeedsReviewCount(): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions
        WHERE occurredAt >= :startOfDay AND occurredAt < :endOfDay
          AND type = 'EXPENSE' AND excludeFromReports = 0
        """
    )
    fun observeTodaySpent(startOfDay: Long, endOfDay: Long): Flow<Long>

    @Query(
        """
        SELECT categoryId, SUM(amountCents) AS total, COUNT(*) AS qty
        FROM transactions
        WHERE yearMonth IN (:yearMonths) AND type = 'EXPENSE' AND excludeFromReports = 0
        GROUP BY categoryId ORDER BY total DESC
        """
    )
    suspend fun categoryTotalsForMonths(yearMonths: List<Int>): List<CategoryTotalRow>

    @Query(
        """
        SELECT dayOfWeek, SUM(amountCents) AS total, COUNT(*) AS qty
        FROM transactions
        WHERE yearMonth = :yearMonth AND type = 'EXPENSE' AND excludeFromReports = 0
        GROUP BY dayOfWeek
        """
    )
    suspend fun weekdayTotals(yearMonth: Int): List<WeekdayTotalRow>

    @Query(
        """
        SELECT merchantNormalized, SUM(amountCents) AS total, COUNT(*) AS qty
        FROM transactions
        WHERE yearMonth = :yearMonth AND type = 'EXPENSE' AND excludeFromReports = 0
          AND merchantNormalized IS NOT NULL
        GROUP BY merchantNormalized ORDER BY total DESC
        """
    )
    suspend fun merchantTotals(yearMonth: Int): List<MerchantTotalRow>

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE yearMonth = :yearMonth AND categoryId = :categoryId AND excludeFromReports = 0
        """
    )
    suspend fun countByCategory(yearMonth: Int, categoryId: String): Int

    @Query(
        """
        SELECT COALESCE(SUM(amountCents), 0) FROM transactions
        WHERE yearMonth = :yearMonth AND type = 'EXPENSE' AND excludeFromReports = 0
          AND amountCents < :thresholdCents
        """
    )
    suspend fun microSpendTotal(yearMonth: Int, thresholdCents: Long): Long

    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE yearMonth = :yearMonth AND type = 'EXPENSE' AND excludeFromReports = 0
          AND amountCents < :thresholdCents
        """
    )
    suspend fun microSpendCount(yearMonth: Int, thresholdCents: Long): Int

    @Query("SELECT DISTINCT occurredAt / 86400000 FROM transactions WHERE type = 'EXPENSE' AND excludeFromReports = 0 ORDER BY 1 DESC LIMIT 60")
    suspend fun recentSpendDayBuckets(): List<Long>

    @Query(
        """
        SELECT * FROM transactions
        WHERE type = 'EXPENSE' AND excludeFromReports = 0 AND yearMonth IN (:yearMonths)
        ORDER BY occurredAt DESC
        """
    )
    suspend fun expensesForMonths(yearMonths: List<Int>): List<TransactionEntity>

    @Query(
        """
        SELECT yearMonth, type, SUM(amountCents) AS total
        FROM transactions
        WHERE yearMonth IN (:yearMonths) AND excludeFromReports = 0
        GROUP BY yearMonth, type
        """
    )
    suspend fun monthlyTotalsForMonths(yearMonths: List<Int>): List<MonthTypeTotalRow>

    @Query(
        """
        SELECT yearMonth, categoryId, SUM(amountCents) AS total, COUNT(*) AS qty
        FROM transactions
        WHERE yearMonth IN (:yearMonths) AND type = 'EXPENSE' AND excludeFromReports = 0
        GROUP BY yearMonth, categoryId
        """
    )
    suspend fun categoryTotalsByMonth(yearMonths: List<Int>): List<CategoryTotalByMonthRow>

    @Query(
        """
        SELECT paymentMethod, SUM(amountCents) AS total, COUNT(*) AS qty
        FROM transactions
        WHERE yearMonth = :yearMonth AND type = 'EXPENSE' AND excludeFromReports = 0
        GROUP BY paymentMethod
        """
    )
    suspend fun paymentMethodTotals(yearMonth: Int): List<PaymentMethodTotalRow>

    @Query(
        """
        SELECT MAX(amountCents) FROM transactions
        WHERE yearMonth = :yearMonth AND type = 'EXPENSE' AND excludeFromReports = 0
        """
    )
    suspend fun maxExpense(yearMonth: Int): Long?

    @Query(
        """
        SELECT COUNT(DISTINCT occurredAt / 86400000) FROM transactions
        WHERE yearMonth = :yearMonth AND type = 'EXPENSE' AND excludeFromReports = 0
        """
    )
    suspend fun distinctSpendDaysInMonth(yearMonth: Int): Int

    @Query(
        """
        SELECT (yearMonth / 100) AS year, type, SUM(amountCents) AS total
        FROM transactions
        WHERE (yearMonth / 100) IN (:years) AND excludeFromReports = 0
        GROUP BY year, type
        """
    )
    suspend fun yearlyTotalsForYears(years: List<Int>): List<YearTypeTotalRow>

    @Query(
        """
        SELECT (yearMonth / 100) AS year, categoryId, SUM(amountCents) AS total, COUNT(*) AS qty
        FROM transactions
        WHERE (yearMonth / 100) IN (:years) AND type = 'EXPENSE' AND excludeFromReports = 0
        GROUP BY year, categoryId
        """
    )
    suspend fun categoryTotalsByYear(years: List<Int>): List<CategoryTotalByYearRow>

    /** Lançamentos gerados a partir de contas fixas (`recurrenceGroupId`) já pagos no mês. */
    @Query("SELECT * FROM transactions WHERE recurrenceGroupId IN (:ruleIds) AND yearMonth = :yearMonth")
    fun observePaymentsForMonth(ruleIds: List<String>, yearMonth: Int): Flow<List<TransactionEntity>>

    /** Busca por descrição em todos os meses (não só no mês selecionado na tela). */
    @Query("SELECT * FROM transactions WHERE description LIKE '%' || :query || '%' ORDER BY occurredAt DESC")
    fun search(query: String): Flow<List<TransactionEntity>>
}

data class CategoryTotalRow(
    val categoryId: String,
    val total: Long,
    val qty: Int
)

data class WeekdayTotalRow(
    val dayOfWeek: Int,
    val total: Long,
    val qty: Int
)

data class MerchantTotalRow(
    val merchantNormalized: String?,
    val total: Long,
    val qty: Int
)

data class MonthTypeTotalRow(
    val yearMonth: Int,
    val type: String,
    val total: Long
)

data class CategoryTotalByMonthRow(
    val yearMonth: Int,
    val categoryId: String,
    val total: Long,
    val qty: Int
)

data class PaymentMethodTotalRow(
    val paymentMethod: String?,
    val total: Long,
    val qty: Int
)

data class YearTypeTotalRow(
    val year: Int,
    val type: String,
    val total: Long
)

data class CategoryTotalByYearRow(
    val year: Int,
    val categoryId: String,
    val total: Long,
    val qty: Int
)
