package br.com.financas.feature.voice.gateway

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.financas.core.data.repository.AccountRepository
import br.com.financas.core.data.repository.CategoryRepository
import br.com.financas.core.data.repository.CategoryRuleRepository
import br.com.financas.core.data.repository.TransactionRepository
import br.com.financas.core.database.AppDatabase
import br.com.financas.core.model.EntrySource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Testa o gateway ponta a ponta com um Room in-memory real — é o ponto por
 * onde as 8 rotas de entrada passam (regra §11 do CLAUDE.md), então vale
 * mais um teste de integração do que mocks de cada repository.
 */
@RunWith(RobolectricTestRunner::class)
class QuickEntryGatewayTest {

    private lateinit var db: AppDatabase
    private lateinit var gateway: QuickEntryGateway

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val clock = Clock.fixed(Instant.parse("2026-08-25T14:30:00Z"), ZoneOffset.UTC)
        val transactionRepository = TransactionRepository(db.transactionDao(), clock)
        val categoryRepository = CategoryRepository(db.categoryDao(), db.categoryRuleDao())
        val categoryRuleRepository = CategoryRuleRepository(db.categoryRuleDao(), clock)
        val accountRepository = AccountRepository(db.accountDao())
        gateway = QuickEntryGateway(
            transactionRepository, categoryRepository, categoryRuleRepository, accountRepository, clock
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `gasto por voz e gravado direto, sem tela de confirmacao`() = runTest {
        val outcome = gateway.ingest("gastei 20 reais de pastel", EntrySource.VOICE)

        assertThat(outcome).isInstanceOf(IngestOutcome.Recorded::class.java)
        val recorded = outcome as IngestOutcome.Recorded
        assertThat(recorded.amountCents).isEqualTo(2000L)
        assertThat(recorded.isExpense).isTrue()
        assertThat(recorded.categoryName).isEqualTo("Alimentação")
        assertThat(recorded.needsReview).isFalse()

        val saved = db.transactionDao().getById(recorded.transactionId)
        assertThat(saved).isNotNull()
        assertThat(saved!!.rawInput).isEqualTo("gastei 20 reais de pastel")
        assertThat(saved.source).isEqualTo(EntrySource.VOICE)
    }

    @Test
    fun `frase sem valor nao grava nada`() = runTest {
        val outcome = gateway.ingest("um dinheirão qualquer", EntrySource.VOICE)
        assertThat(outcome).isInstanceOf(IngestOutcome.NotUnderstood::class.java)
    }

    @Test
    fun `regra aprendida pelo usuario tem prioridade no proximo lancamento`() = runTest {
        gateway.ingest("gastei 10 na xandoca", EntrySource.MANUAL)
        val categoryRuleRepository = CategoryRuleRepository(db.categoryRuleDao(), Clock.systemUTC())
        categoryRuleRepository.learn(keyword = "xandoca", categoryId = "cat_entretenimento")

        val outcome = gateway.ingest("gastei 15 na xandoca", EntrySource.MANUAL) as IngestOutcome.Recorded
        assertThat(outcome.categoryName).isEqualTo("Entretenimento")
    }
}
