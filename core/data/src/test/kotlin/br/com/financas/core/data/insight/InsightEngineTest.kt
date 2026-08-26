package br.com.financas.core.data.insight

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.financas.core.data.repository.AccountRepository
import br.com.financas.core.data.repository.BudgetRepository
import br.com.financas.core.data.repository.CategoryRepository
import br.com.financas.core.data.seed.CategorySeeder
import br.com.financas.core.database.AppDatabase
import br.com.financas.core.database.entity.AccountEntity
import br.com.financas.core.database.entity.TransactionEntity
import br.com.financas.core.model.AccountKind
import br.com.financas.core.model.EntrySource
import br.com.financas.core.model.InsightType
import br.com.financas.core.model.TransactionType
import br.com.financas.nlu.category.DefaultCategories
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class InsightEngineTest {

    private lateinit var db: AppDatabase
    private lateinit var engine: InsightEngine

    private val zone = ZoneOffset.UTC
    private val clock = Clock.fixed(Instant.parse("2026-08-25T14:30:00Z"), zone)
    private val august = 202608

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        db.accountDao().insert(AccountEntity("default", "Carteira", AccountKind.CASH, 0, null, null, 0))
        db.categoryDao().insertAll(CategorySeeder.categoryEntities())
        db.categoryRuleDao().insertAll(CategorySeeder.categoryRuleEntities())

        val categoryRepository = CategoryRepository(db.categoryDao(), db.categoryRuleDao(), clock)
        val budgetRepository = BudgetRepository(db.budgetDao())
        engine = InsightEngine(db.transactionDao(), categoryRepository, budgetRepository, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `categoria com gasto muito acima da media dispara CATEGORY_SPIKE`() = runTest {
        // maio, junho e julho: ~R$300 em Alimentação. Agosto: R$500 — salto de 67%.
        insertExpense(cents = 30_000, category = DefaultCategories.Id.FOOD, date = LocalDate.of(2026, 5, 20))
        insertExpense(cents = 30_000, category = DefaultCategories.Id.FOOD, date = LocalDate.of(2026, 6, 20))
        insertExpense(cents = 30_000, category = DefaultCategories.Id.FOOD, date = LocalDate.of(2026, 7, 20))
        insertExpense(cents = 50_000, category = DefaultCategories.Id.FOOD, date = LocalDate.of(2026, 8, 20))

        val insights = engine.generate(august)

        val spike = insights.firstOrNull { it.type == InsightType.CATEGORY_SPIKE }
        assertThat(spike).isNotNull()
        assertThat(spike!!.relatedCategoryId).isEqualTo(DefaultCategories.Id.FOOD)
    }

    @Test
    fun `tres cobrancas mensais do mesmo valor formam uma assinatura`() = runTest {
        insertExpense(cents = 5_500, category = DefaultCategories.Id.ENTERTAINMENT, date = LocalDate.of(2026, 6, 25), merchant = "NETFLIX")
        insertExpense(cents = 5_500, category = DefaultCategories.Id.ENTERTAINMENT, date = LocalDate.of(2026, 7, 25), merchant = "NETFLIX")
        insertExpense(cents = 5_500, category = DefaultCategories.Id.ENTERTAINMENT, date = LocalDate.of(2026, 8, 25), merchant = "NETFLIX")

        val insights = engine.generate(august)

        val subscription = insights.firstOrNull { it.type == InsightType.SUBSCRIPTION_TOTAL }
        assertThat(subscription).isNotNull()
        assertThat(subscription!!.impactCents).isEqualTo(5_500L)
    }

    @Test
    fun `mes sem receita nao gera insight de taxa de poupanca`() = runTest {
        insertExpense(cents = 1_000, category = DefaultCategories.Id.FOOD, date = LocalDate.of(2026, 8, 20))
        val insights = engine.generate(august)
        assertThat(insights.none { it.type == InsightType.SAVINGS_RATE }).isTrue()
    }

    private suspend fun insertExpense(cents: Long, category: String, date: LocalDate, merchant: String? = null) {
        val millis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        db.transactionDao().insert(
            TransactionEntity(
                id = "tx_${System.nanoTime()}_${(0..999999).random()}",
                amountCents = cents,
                type = TransactionType.EXPENSE,
                description = merchant ?: "despesa",
                rawInput = null,
                categoryId = category,
                accountId = "default",
                occurredAt = millis,
                createdAt = millis,
                yearMonth = date.year * 100 + date.monthValue,
                dayOfWeek = date.dayOfWeek.value,
                paymentMethod = null,
                source = EntrySource.MANUAL,
                confidence = null,
                merchantNormalized = merchant
            )
        )
    }
}
