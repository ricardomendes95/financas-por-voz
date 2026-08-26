package br.com.financas.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.financas.core.database.AppDatabase
import br.com.financas.core.database.entity.AccountEntity
import br.com.financas.core.model.AccountKind
import br.com.financas.core.model.EntrySource
import br.com.financas.core.model.TransactionDraft
import br.com.financas.core.model.TransactionType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class RecurringRuleRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: RecurringRuleRepository
    private lateinit var transactionRepository: TransactionRepository

    private val zone = ZoneOffset.UTC
    private val clock = Clock.fixed(Instant.parse("2026-08-25T14:30:00Z"), zone)
    private val august = 202608
    private val categoryId = "cat_moradia"
    private val accountId = "default"

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        db.accountDao().insert(AccountEntity(accountId, "Carteira", AccountKind.CASH, 0, null, null, 0))

        transactionRepository = TransactionRepository(db.transactionDao(), clock)
        repository = RecurringRuleRepository(db.recurringRuleDao(), db.transactionDao(), transactionRepository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `regra recem criada aparece pendente no mes`() = runTest {
        val ruleId = repository.create("Água", 8000, categoryId, TransactionType.EXPENSE, dayOfMonth = 10)

        val statuses = repository.observeActiveWithStatus(august).first()

        assertThat(statuses).hasSize(1)
        assertThat(statuses.first().rule.id).isEqualTo(ruleId)
        assertThat(statuses.first().paidTransaction).isNull()
    }

    @Test
    fun `confirmar pagamento marca a regra como paga no mes`() = runTest {
        val ruleId = repository.create("Internet", 12000, categoryId, TransactionType.EXPENSE, dayOfMonth = 5)

        val transactionId = repository.confirmPayment(ruleId, amountCents = 12000, occurredAt = clock.millis(), accountId = accountId)

        val statuses = repository.observeActiveWithStatus(august).first()
        assertThat(statuses.first().paidTransaction?.id).isEqualTo(transactionId)
        assertThat(statuses.first().paidTransaction?.amountCents).isEqualTo(12000)
        assertThat(statuses.first().paidTransaction?.recurrenceGroupId).isEqualTo(ruleId)
    }

    @Test
    fun `confirmar com valor diferente atualiza o valor padrao da regra`() = runTest {
        val ruleId = repository.create("Financiamento", 150000, categoryId, TransactionType.EXPENSE, dayOfMonth = 15)

        repository.confirmPayment(ruleId, amountCents = 150300, occurredAt = clock.millis(), accountId = accountId)

        val updated = db.recurringRuleDao().getById(ruleId)
        assertThat(updated?.amountCents).isEqualTo(150300)
    }

    @Test
    fun `arquivar some da lista de ativas mas mantem o lancamento historico`() = runTest {
        val ruleId = repository.create("FIES", 45000, categoryId, TransactionType.EXPENSE, dayOfMonth = 20)
        val transactionId = repository.confirmPayment(ruleId, amountCents = 45000, occurredAt = clock.millis(), accountId = accountId)

        repository.archive(ruleId)

        val statuses = repository.observeActiveWithStatus(august).first()
        assertThat(statuses).isEmpty()
        assertThat(db.transactionDao().getById(transactionId)).isNotNull()
    }

    @Test
    fun `associar lancamento existente marca a regra como paga sem criar novo lancamento`() = runTest {
        val ruleId = repository.create("FIES", 45000, categoryId, TransactionType.EXPENSE, dayOfMonth = 10)
        // Simula um lançamento já existente antes da conta fixa ser cadastrada (ex.: importado do extrato).
        val existingTransactionId = transactionRepository.create(
            draft = TransactionDraft(
                amountCents = 45000,
                type = TransactionType.EXPENSE,
                description = "FIES",
                categoryId = categoryId,
                accountId = accountId,
                occurredAt = clock.millis(),
                paymentMethod = null
            ),
            source = EntrySource.IMPORT
        )

        repository.linkExistingPayment(ruleId, existingTransactionId)

        val statuses = repository.observeActiveWithStatus(august).first()
        assertThat(statuses.first().paidTransaction?.id).isEqualTo(existingTransactionId)
        assertThat(db.transactionDao().getById(existingTransactionId)?.recurrenceGroupId).isEqualTo(ruleId)
    }
}
