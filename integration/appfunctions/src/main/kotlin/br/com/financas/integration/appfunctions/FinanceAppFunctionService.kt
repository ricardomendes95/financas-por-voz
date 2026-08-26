package br.com.financas.integration.appfunctions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import br.com.financas.core.common.MoneyFormatter
import br.com.financas.core.common.YearMonthUtils
import br.com.financas.core.data.repository.CategoryRepository
import br.com.financas.core.data.repository.ReportsRepository
import br.com.financas.core.data.repository.TransactionRepository
import br.com.financas.core.model.Category
import br.com.financas.core.model.EntrySource
import br.com.financas.feature.voice.gateway.IngestOutcome
import br.com.financas.feature.voice.gateway.QuickEntryGateway
import br.com.financas.feature.voice.notification.EntryNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * Rota 1 (§5.2) — o único canal que fala com o Gemini. O KDoc de cada
 * função É o contrato lido pelo agente para decidir se chama o app: escrito
 * como se fosse um prompt. Bônus, não fundação (§0.3) — se o Android 16 do
 * S23 não indexar isso, as rotas 4-8 entregam a experiência completa
 * sozinhas.
 */
@RequiresApi(36)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "FinanceAppFunctionService",
    appFunctionXmlFileName = "finance_app_function_service"
)
abstract class BaseFinanceAppFunctionService : AppFunctionService() {

    @Inject internal lateinit var gateway: QuickEntryGateway
    @Inject internal lateinit var entryNotifier: EntryNotifier
    @Inject internal lateinit var transactionRepository: TransactionRepository
    @Inject internal lateinit var reportsRepository: ReportsRepository
    @Inject internal lateinit var categoryRepository: CategoryRepository
    @Inject internal lateinit var clock: Clock

    /**
     * Registra uma despesa (saída de dinheiro) nas finanças pessoais do
     * usuário. Use quando o usuário disser que gastou, pagou, comprou algo
     * ou quer adicionar uma despesa. Aceita a frase completa em linguagem
     * natural — o app extrai valor, categoria e data automaticamente.
     *
     * @param naturalLanguageInput A frase completa dita pelo usuário, sem
     *   modificações. Ex.: "20 reais de pastel no dia 24".
     * @return Resumo do lançamento criado, para ser lido de volta ao usuário.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun addExpense(naturalLanguageInput: String): EntryResult =
        ingestAndSummarize(naturalLanguageInput)

    /**
     * Registra uma receita (entrada de dinheiro). Use quando o usuário
     * disser que recebeu, ganhou, ou que algo caiu na conta.
     *
     * @param naturalLanguageInput A frase completa dita pelo usuário.
     * @return Resumo do lançamento criado, para ser lido de volta ao usuário.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun addIncome(naturalLanguageInput: String): EntryResult =
        ingestAndSummarize(naturalLanguageInput)

    /**
     * Consulta quanto o usuário gastou este mês. Use para perguntas como
     * "quanto gastei esse mês" ou "qual meu saldo".
     *
     * @param naturalLanguageQuery A pergunta completa dita pelo usuário.
     * @return Resposta curta e falável com o total gasto e a maior categoria.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun querySpending(naturalLanguageQuery: String): SpendingSummary {
        val yearMonth = YearMonthUtils.currentYearMonth(clock.zone)
        val categories = categoryRepository.observeActive().first().associateBy(Category::id)
        val report = reportsRepository.categoryReport(yearMonth)
        val total = report.sumOf { it.totalCents }
        val top = report.maxByOrNull { it.totalCents }
        val topName = top?.let { categories[it.categoryId]?.name }

        val summary = if (top != null && topName != null) {
            "Você gastou ${MoneyFormatter.format(total)} este mês. A maior categoria foi $topName, com ${MoneyFormatter.format(top.totalCents)}."
        } else {
            "Você ainda não tem gastos registrados este mês."
        }
        return SpendingSummary(summary = summary, totalCents = total)
    }

    /**
     * Cria uma nova categoria de gastos com o nome informado.
     *
     * @param name Nome da categoria a ser criada.
     * @return A categoria recém-criada.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createCategory(name: String): CategoryResult {
        val id = "user_cat_${UUID.randomUUID()}"
        categoryRepository.createUserCategory(id = id, name = name)
        return CategoryResult(categoryId = id, name = name)
    }

    private suspend fun ingestAndSummarize(naturalLanguageInput: String): EntryResult {
        val outcome = gateway.ingest(naturalLanguageInput, EntrySource.VOICE)
        entryNotifier.notifyOutcome(outcome)
        return when (outcome) {
            is IngestOutcome.Recorded -> {
                val verb = if (outcome.isExpense) "Despesa" else "Receita"
                EntryResult(
                    success = true,
                    summary = "$verb de ${MoneyFormatter.format(outcome.amountCents)} em " +
                        "${outcome.categoryName} registrada."
                )
            }
            is IngestOutcome.NotUnderstood -> EntryResult(
                success = false,
                summary = "Não entendi o valor em \"${outcome.rawText}\"."
            )
        }
    }
}
