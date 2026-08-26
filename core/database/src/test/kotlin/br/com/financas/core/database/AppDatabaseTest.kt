package br.com.financas.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.financas.core.database.entity.AccountEntity
import br.com.financas.core.database.entity.CategoryEntity
import br.com.financas.core.database.entity.CategoryRuleEntity
import br.com.financas.core.database.entity.TransactionEntity
import br.com.financas.core.model.AccountKind
import br.com.financas.core.model.EntrySource
import br.com.financas.core.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Valida que o schema completo (9 entidades da §3.1) abre num Room
 * in-memory e que os DAOs básicos gravam/leem corretamente — inclusive a
 * agregação mensal, que precisa continuar em SQL puro (regra §11).
 */
@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `schema abre e DAOs estao todos acessiveis`() {
        assertThat(db.transactionDao()).isNotNull()
        assertThat(db.categoryDao()).isNotNull()
        assertThat(db.categoryRuleDao()).isNotNull()
        assertThat(db.accountDao()).isNotNull()
        assertThat(db.budgetDao()).isNotNull()
        assertThat(db.tagDao()).isNotNull()
        assertThat(db.recurringRuleDao()).isNotNull()
    }

    @Test
    fun `inserir e ler categoria`() = runBlocking {
        val category = CategoryEntity(
            id = "cat_alimentacao",
            name = "Alimentação",
            icon = "restaurant",
            colorArgb = 0xFFF97316.toInt(),
            type = TransactionType.EXPENSE,
            parentId = null,
            isSystem = true,
            sortOrder = 0,
            archivedAt = null
        )
        db.categoryDao().insertAll(listOf(category))

        val loaded = db.categoryDao().getById("cat_alimentacao")
        assertThat(loaded).isEqualTo(category)
    }

    @Test
    fun `agregacao mensal soma em SQL por tipo`() = runBlocking {
        seedAccountAndCategory()

        insertTransaction(amountCents = 2000, type = TransactionType.EXPENSE, yearMonth = 202608)
        insertTransaction(amountCents = 3000, type = TransactionType.EXPENSE, yearMonth = 202608)
        insertTransaction(amountCents = 500_00, type = TransactionType.INCOME, yearMonth = 202608)
        insertTransaction(amountCents = 999, type = TransactionType.EXPENSE, yearMonth = 202607)

        val summary = db.transactionDao().observeMonthlySummary(202608).first()
        val expense = summary.first { it.type == "EXPENSE" }.total
        val income = summary.first { it.type == "INCOME" }.total

        assertThat(expense).isEqualTo(5000L)
        assertThat(income).isEqualTo(50000L)
    }

    private suspend fun seedAccountAndCategory() {
        db.accountDao().insert(
            AccountEntity(
                id = "default",
                name = "Carteira",
                kind = AccountKind.CASH,
                openingBalanceCents = 0,
                closingDay = null,
                dueDay = null,
                colorArgb = 0xFF64748B.toInt()
            )
        )
        db.categoryDao().insertAll(
            listOf(
                CategoryEntity(
                    id = "cat_outros",
                    name = "Outros",
                    icon = "more_horiz",
                    colorArgb = 0xFF94A3B8.toInt(),
                    type = null,
                    parentId = null,
                    isSystem = true,
                    sortOrder = 0,
                    archivedAt = null
                )
            )
        )
        db.categoryRuleDao().insert(
            CategoryRuleEntity(
                id = "rule_1",
                keyword = "teste",
                categoryId = "cat_outros",
                weight = 10,
                isUserDefined = false
            )
        )
    }

    private suspend fun insertTransaction(amountCents: Long, type: TransactionType, yearMonth: Int) {
        db.transactionDao().insert(
            TransactionEntity(
                id = "tx_${System.nanoTime()}",
                amountCents = amountCents,
                type = type,
                description = "lançamento de teste",
                rawInput = null,
                categoryId = "cat_outros",
                accountId = "default",
                occurredAt = 0L,
                createdAt = 0L,
                yearMonth = yearMonth,
                dayOfWeek = 1,
                paymentMethod = null,
                source = EntrySource.MANUAL,
                confidence = null
            )
        )
    }
}
