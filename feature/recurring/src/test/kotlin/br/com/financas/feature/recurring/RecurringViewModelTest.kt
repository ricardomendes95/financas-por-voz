package br.com.financas.feature.recurring

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import br.com.financas.core.data.repository.CategoryRepository
import br.com.financas.core.data.repository.RecurringRuleRepository
import br.com.financas.core.data.repository.TransactionRepository
import br.com.financas.core.database.AppDatabase
import br.com.financas.core.data.seed.CategorySeeder
import br.com.financas.core.database.entity.AccountEntity
import br.com.financas.core.model.AccountKind
import br.com.financas.core.model.TransactionType
import br.com.financas.nlu.category.DefaultCategories.Id
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RecurringViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var recurringRuleRepository: RecurringRuleRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var viewModel: RecurringViewModel

    // Dia 25 de agosto/2026 — usado para exercitar o cálculo de "Atrasado".
    private val clock = Clock.fixed(Instant.parse("2026-08-25T14:30:00Z"), ZoneOffset.UTC)
    private val categoryId = Id.HOUSING

    @Before
    fun setUp() = runTest {
        // viewModelScope roda no Dispatchers.Main; sem trocá-lo por um dispatcher de
        // teste, o StateFlow do stateIn nunca avança dentro do runTest e o Turbine
        // estoura timeout esperando o primeiro item.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        db.accountDao().insert(AccountEntity("default", "Carteira", AccountKind.CASH, 0, null, null, 0))
        db.categoryDao().insertAll(CategorySeeder.categoryEntities())

        val categoryRepository = CategoryRepository(db.categoryDao(), db.categoryRuleDao(), clock)
        transactionRepository = TransactionRepository(db.transactionDao(), clock)
        recurringRuleRepository = RecurringRuleRepository(db.recurringRuleDao(), db.transactionDao(), transactionRepository)

        viewModel = RecurringViewModel(recurringRuleRepository, transactionRepository, categoryRepository, clock)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `conta com vencimento ja passado no mes corrente aparece atrasada`() = runTest {
        recurringRuleRepository.create("Água", 8000, categoryId, TransactionType.EXPENSE, dayOfMonth = 10)

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.isLoading || state.rows.isEmpty()) state = awaitItem()

            assertThat(state.yearMonth).isEqualTo(202608)
            assertThat(state.rows.first().isOverdue).isTrue()
        }
    }

    @Test
    fun `mesma conta pendente em mes anterior nao aparece atrasada`() = runTest {
        recurringRuleRepository.create("Água", 8000, categoryId, TransactionType.EXPENSE, dayOfMonth = 10)

        viewModel.onPreviousMonth()

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.isLoading || state.rows.isEmpty()) state = awaitItem()

            assertThat(state.yearMonth).isEqualTo(202607)
            assertThat(state.rows.first().isOverdue).isFalse()
        }
    }

    @Test
    fun `associar lancamento existente marca a conta como paga sem duplicar`() = runTest {
        val ruleId = recurringRuleRepository.create("FIES", 45000, categoryId, TransactionType.EXPENSE, dayOfMonth = 10)
        val existingTransactionId = transactionRepository.create(
            draft = br.com.financas.core.model.TransactionDraft(
                amountCents = 45000,
                type = TransactionType.EXPENSE,
                description = "FIES",
                categoryId = categoryId,
                accountId = "default",
                occurredAt = clock.millis(),
                paymentMethod = null
            ),
            source = br.com.financas.core.model.EntrySource.IMPORT
        )

        recurringRuleRepository.linkExistingPayment(ruleId, existingTransactionId)

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.isLoading || state.rows.isEmpty()) state = awaitItem()

            assertThat(state.rows.first().isPaid).isTrue()
            assertThat(state.rows.first().paidTransactionId).isEqualTo(existingTransactionId)
        }
    }
}
