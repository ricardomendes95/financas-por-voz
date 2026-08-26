package br.com.financas.core.data.insight

import br.com.financas.core.common.MoneyFormatter
import br.com.financas.core.data.mapper.toDomain
import br.com.financas.core.data.repository.BudgetRepository
import br.com.financas.core.data.repository.CategoryRepository
import br.com.financas.core.database.dao.TransactionDao
import br.com.financas.core.model.Insight
import br.com.financas.core.model.InsightType
import br.com.financas.nlu.category.DefaultCategories
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gera os cards acionáveis do Dashboard (§7.3), ranqueados por impacto em
 * R$. Recalculado ao abrir o Dashboard — não é um serviço em segundo plano.
 */
@Singleton
class InsightEngine @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val clock: Clock
) {
    private val zone: ZoneId get() = clock.zone

    suspend fun generate(yearMonth: Int): List<Insight> {
        val categories = categoryRepository.observeActive().first().associateBy { it.id }
        val currentTotals = transactionDao.categoryTotalsForMonths(listOf(yearMonth))
        val currentByCategory = currentTotals.associateBy { it.categoryId }
        val totalExpense = currentTotals.sumOf { it.total }
        val summaryRows = transactionDao.observeMonthlySummary(yearMonth)
        val totalIncome = summaryRows.first().firstOrNull { it.type == "INCOME" }?.total ?: 0L

        val insights = mutableListOf<Insight>()

        categorySpike(yearMonth, currentByCategory, categories)?.let { insights += it }
        paceWarning(yearMonth, totalExpense)?.let { insights += it }

        val recurring = RecurrenceDetector.detect(
            transactionDao.expensesForMonths(listOf(yearMonth) + lastNMonths(yearMonth, 5)).map { it.toDomain() }
        )
        subscriptionTotal(recurring)?.let { insights += it }
        zombieSubscription(recurring, categories)?.let { insights += it }
        priceCreep(recurring)?.let { insights += it }

        microSpend(yearMonth, totalExpense)?.let { insights += it }
        expensiveDay(yearMonth, totalExpense)?.let { insights += it }
        topMerchant(yearMonth)?.let { insights += it }
        noSpendStreak()?.let { insights += it }
        uncategorized(yearMonth)?.let { insights += it }
        savingsRate(totalIncome, totalExpense)?.let { insights += it }
        weekendRatio(yearMonth, totalExpense)?.let { insights += it }

        return insights.sortedByDescending { it.impactCents }
    }

    private suspend fun categorySpike(
        yearMonth: Int,
        currentByCategory: Map<String, br.com.financas.core.database.dao.CategoryTotalRow>,
        categories: Map<String, br.com.financas.core.model.Category>
    ): Insight? {
        val historical = transactionDao.categoryTotalsForMonths(lastNMonths(yearMonth, 3))
        val historicalAverage = historical
            .groupBy { it.categoryId }
            .mapValues { (_, rows) -> rows.sumOf { it.total } / 3.0 }

        return currentByCategory.values
            .mapNotNull { row ->
                val average = historicalAverage[row.categoryId] ?: return@mapNotNull null
                if (average <= 0.0 || row.total <= average * 1.3) return@mapNotNull null
                val percent = ((row.total - average) / average * 100).toInt()
                val name = categories[row.categoryId]?.name ?: return@mapNotNull null
                val diff = (row.total - average).toLong()
                Insight(
                    type = InsightType.CATEGORY_SPIKE,
                    message = "$name está $percent% acima da sua média. ${MoneyFormatter.format(diff)} a mais que o normal.",
                    impactCents = diff,
                    relatedCategoryId = row.categoryId
                )
            }
            .maxByOrNull { it.impactCents }
    }

    private suspend fun paceWarning(yearMonth: Int, totalExpense: Long): Insight? {
        val budgets = budgetRepository.observeByMonth(yearMonth).first()
        val limit = budgets.firstOrNull { it.categoryId == null }?.limitCents
            ?: budgets.sumOf { it.limitCents }.takeIf { it > 0 }
            ?: return null

        val today = LocalDate.now(clock)
        val ym = YearMonth.of(yearMonth / 100, yearMonth % 100)
        if (ym != YearMonth.from(today)) return null
        val daysElapsed = today.dayOfMonth.coerceAtLeast(1)
        val projected = (totalExpense.toDouble() / daysElapsed * ym.lengthOfMonth()).toLong()
        if (projected <= limit) return null

        val over = projected - limit
        return Insight(
            type = InsightType.PACE_WARNING,
            message = "No ritmo atual você fecha o mês em ${MoneyFormatter.format(projected)} — " +
                "${MoneyFormatter.format(over)} acima do orçado.",
            impactCents = over
        )
    }

    private fun subscriptionTotal(recurring: List<br.com.financas.core.model.RecurringCandidate>): Insight? {
        if (recurring.isEmpty()) return null
        val monthlyTotal = recurring.sumOf { it.averageAmountCents }
        return Insight(
            type = InsightType.SUBSCRIPTION_TOTAL,
            message = "${recurring.size} assinaturas ativas somam ${MoneyFormatter.format(monthlyTotal)}/mês.",
            impactCents = monthlyTotal
        )
    }

    private fun zombieSubscription(
        recurring: List<br.com.financas.core.model.RecurringCandidate>,
        categories: Map<String, br.com.financas.core.model.Category>
    ): Insight? {
        val now = clock.millis()
        val zombie = recurring
            .filter { candidate ->
                val category = categories[candidate.categoryId]
                val isEntertainmentOrServices = category?.id == DefaultCategories.Id.ENTERTAINMENT ||
                    category?.id == DefaultCategories.Id.SERVICES
                val daysSinceLast = (now - candidate.lastOccurredAt) / 86_400_000L
                isEntertainmentOrServices && daysSinceLast > 45
            }
            .maxByOrNull { it.averageAmountCents } ?: return null

        return Insight(
            type = InsightType.ZOMBIE_SUB,
            message = "Você paga ${zombie.label} há ${zombie.occurrences} meses. Ainda usa?",
            impactCents = zombie.averageAmountCents,
            relatedCategoryId = zombie.categoryId
        )
    }

    private fun priceCreep(recurring: List<br.com.financas.core.model.RecurringCandidate>): Insight? {
        val creeping = recurring
            .filter { val previous = it.previousAmountCents; previous != null && it.lastAmountCents > previous * 1.10 }
            .maxByOrNull { it.lastAmountCents - (it.previousAmountCents ?: 0L) } ?: return null

        val diff = creeping.lastAmountCents - (creeping.previousAmountCents ?: 0L)
        return Insight(
            type = InsightType.PRICE_CREEP,
            message = "${creeping.label} subiu ${MoneyFormatter.format(diff)} desde a última cobrança.",
            impactCents = diff,
            relatedCategoryId = creeping.categoryId
        )
    }

    private suspend fun microSpend(yearMonth: Int, totalExpense: Long): Insight? {
        if (totalExpense <= 0) return null
        val microTotal = transactionDao.microSpendTotal(yearMonth, 3_000)
        val ratio = microTotal.toDouble() / totalExpense
        if (ratio <= 0.10) return null
        val count = transactionDao.microSpendCount(yearMonth, 3_000)
        val percent = (ratio * 100).toInt()
        return Insight(
            type = InsightType.MICRO_SPEND,
            message = "${MoneyFormatter.format(microTotal)} em $count gastos pequenos — $percent% do seu mês.",
            impactCents = microTotal
        )
    }

    private suspend fun expensiveDay(yearMonth: Int, totalExpense: Long): Insight? {
        if (totalExpense <= 0) return null
        val rows = transactionDao.weekdayTotals(yearMonth)
        val top = rows.maxByOrNull { it.total } ?: return null
        val ratio = top.total.toDouble() / totalExpense
        if (ratio <= 0.25) return null
        val percent = (ratio * 100).toInt()
        return Insight(
            type = InsightType.EXPENSIVE_DAY,
            message = "${weekdayPluralName(top.dayOfWeek)} concentram $percent% dos seus gastos.",
            impactCents = top.total
        )
    }

    private suspend fun topMerchant(yearMonth: Int): Insight? {
        val top = transactionDao.merchantTotals(yearMonth).firstOrNull { it.qty >= 2 } ?: return null
        val name = top.merchantNormalized ?: return null
        return Insight(
            type = InsightType.TOP_MERCHANT,
            message = "$name: ${top.qty} lançamentos, ${MoneyFormatter.format(top.total)} este mês.",
            impactCents = top.total
        )
    }

    private suspend fun noSpendStreak(): Insight? {
        val today = Instant.ofEpochMilli(clock.millis()).atZone(zone).toLocalDate()
        val spendDays = transactionDao.recentSpendDayBuckets().map { it * 86_400_000L }
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .toSet()

        var streak = 0
        var cursor = today
        while (cursor !in spendDays && ChronoUnit.DAYS.between(cursor, today) < 60) {
            streak++
            cursor = cursor.minusDays(1)
        }
        if (streak < 3) return null

        return Insight(
            type = InsightType.NO_SPEND_STREAK,
            message = "$streak dias sem gastar. Continue assim.",
            impactCents = 0
        )
    }

    private suspend fun uncategorized(yearMonth: Int): Insight? {
        val count = transactionDao.countByCategory(yearMonth, DefaultCategories.Id.OTHER_EXPENSE)
        if (count <= 5) return null
        val rows = transactionDao.categoryTotalsForMonths(listOf(yearMonth))
        val total = rows.firstOrNull { it.categoryId == DefaultCategories.Id.OTHER_EXPENSE }?.total ?: 0L
        return Insight(
            type = InsightType.UNCATEGORIZED,
            message = "$count lançamentos sem categoria — ${MoneyFormatter.format(total)} invisíveis.",
            impactCents = total,
            relatedCategoryId = DefaultCategories.Id.OTHER_EXPENSE
        )
    }

    private fun savingsRate(totalIncome: Long, totalExpense: Long): Insight? {
        if (totalIncome <= 0) return null
        val rate = ((totalIncome - totalExpense).toDouble() / totalIncome * 100).toInt()
        return Insight(
            type = InsightType.SAVINGS_RATE,
            message = "Você guardou $rate% do que entrou este mês.",
            impactCents = totalIncome - totalExpense
        )
    }

    private suspend fun weekendRatio(yearMonth: Int, totalExpense: Long): Insight? {
        if (totalExpense <= 0) return null
        val rows = transactionDao.weekdayTotals(yearMonth)
        val weekendTotal = rows.filter { it.dayOfWeek == DayOfWeek.SATURDAY.value || it.dayOfWeek == DayOfWeek.SUNDAY.value }
            .sumOf { it.total }
        val ratio = weekendTotal.toDouble() / totalExpense
        if (ratio <= 0.40) return null
        val percent = (ratio * 100).toInt()
        return Insight(
            type = InsightType.WEEKEND_RATIO,
            message = "Fins de semana representam $percent% dos seus gastos.",
            impactCents = weekendTotal
        )
    }

    private fun weekdayPluralName(isoDayOfWeek: Int): String = when (isoDayOfWeek) {
        1 -> "Segundas"
        2 -> "Terças"
        3 -> "Quartas"
        4 -> "Quintas"
        5 -> "Sextas"
        6 -> "Sábados"
        else -> "Domingos"
    }

    private fun lastNMonths(yearMonth: Int, n: Int): List<Int> {
        val year = yearMonth / 100
        val month = yearMonth % 100
        var date = java.time.YearMonth.of(year, month)
        return (1..n).map {
            date = date.minusMonths(1)
            date.year * 100 + date.monthValue
        }
    }
}
