package br.com.financas.nlu

import br.com.financas.nlu.category.DefaultCategories
import br.com.financas.nlu.category.DefaultCategories.Id
import br.com.financas.nlu.model.ParseContext
import br.com.financas.nlu.model.PaymentMethod
import br.com.financas.nlu.model.TransactionType
import br.com.financas.nlu.model.TransactionType.EXPENSE
import br.com.financas.nlu.model.TransactionType.INCOME
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Este corpus é o CONTRATO do módulo :nlu.
 *
 * Se uma mudança no parser derrubar um caso daqui, a mudança está errada —
 * não o teste. Casos novos só entram depois de discutidos.
 *
 * Data de referência fixa: terça-feira, 25/08/2026, 14:30.
 */
class FinanceTextParserTest {

    private val parser = FinanceTextParser()

    private val now: LocalDateTime = LocalDateTime.of(2026, 8, 25, 14, 30)

    private val context = ParseContext(
        now = now,
        categories = DefaultCategories.categories,
        rules = DefaultCategories.rules,
        fallbackExpenseCategoryId = Id.OTHER_EXPENSE,
        fallbackIncomeCategoryId = Id.OTHER_INCOME
    )

    // ---- casos canônicos da especificação ------------------------------------

    data class Case(
        val input: String,
        val cents: Long,
        val type: TransactionType,
        val categoryId: String,
        val date: LocalDate,
        val payment: PaymentMethod? = null
    ) {
        override fun toString() = "\"$input\""
    }

    companion object {
        private val AUG = { d: Int -> LocalDate.of(2026, 8, d) }

        @JvmStatic
        fun canonicalCases(): List<Case> = listOf(
            Case(
                "adicione 20 reais de despesa de pagamento da assinatura de tal app",
                2000, EXPENSE, Id.ENTERTAINMENT, AUG(25)
            ),
            Case(
                "adicione 20 reais no dia 24 gasto com pastel",
                2000, EXPENSE, Id.FOOD, AUG(24)
            ),
            Case("gastei 45,90 no mercado ontem", 4590, EXPENSE, Id.FOOD, AUG(24)),
            Case("recebi 3500 de salário dia 5", 350_000, INCOME, Id.SALARY, AUG(5)),
            Case("paguei 89 reais de internet", 8900, EXPENSE, Id.HOUSING, AUG(25)),
            Case("uber 23 e 50", 2350, EXPENSE, Id.TRANSPORT, AUG(25)),
            Case(
                "torrei cento e vinte reais numa cerveja sexta passada",
                12_000, EXPENSE, Id.ENTERTAINMENT, AUG(21)
            ),
            Case(
                "netflix 55 no crédito",
                5500, EXPENSE, Id.ENTERTAINMENT, AUG(25), PaymentMethod.CREDIT
            ),
            Case("caiu 800 de freela", 80_000, INCOME, Id.FREELANCE, AUG(25)),
            Case(
                "farmácia 32,80 pix",
                3280, EXPENSE, Id.HEALTH, AUG(25), PaymentMethod.PIX
            ),
            Case("1,5k de aluguel dia 10", 150_000, EXPENSE, Id.HOUSING, AUG(10)),
            Case(
                "gasolina 200 no dia 24 de julho",
                20_000, EXPENSE, Id.TRANSPORT, LocalDate.of(2026, 7, 24)
            ),
            Case(
                "vinte reais e cinquenta centavos de açaí",
                2050, EXPENSE, Id.FOOD, AUG(25)
            ),
            Case("R$ 1.234,56 de conserto do carro", 123_456, EXPENSE, Id.TRANSPORT, AUG(25)),
            Case("almoço 32 anteontem", 3200, EXPENSE, Id.FOOD, AUG(23)),
            Case("comprei um tênis de 350 reais", 35_000, EXPENSE, Id.SHOPPING, AUG(25)),
            Case("30 conto de ração pro cachorro", 3000, EXPENSE, Id.PETS, AUG(25)),
            Case("entrou 1200 de reembolso", 120_000, INCOME, Id.REFUND, AUG(25)),
            Case("paguei 450 de condomínio 05/08", 45_000, EXPENSE, Id.HOUSING, AUG(5)),
            Case("uber 18 há 3 dias", 1800, EXPENSE, Id.TRANSPORT, AUG(22))
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("canonicalCases")
    fun `frases canônicas são interpretadas corretamente`(case: Case) {
        val result = parser.parse(case.input, context)
        val draft = result.draft

        assertNotNull(draft) { "parser não produziu lançamento para: ${case.input}" }
        requireNotNull(draft)

        assertEquals(case.cents, draft.amountCents, "valor incorreto")
        assertEquals(case.type, draft.type, "tipo incorreto")
        assertEquals(case.categoryId, draft.categoryId, "categoria incorreta")
        assertEquals(case.date, draft.occurredAt.toLocalDate(), "data incorreta")
        if (case.payment != null) {
            assertEquals(case.payment, draft.paymentMethod, "forma de pagamento incorreta")
        }
    }

    // ---- regras de negócio isoladas ------------------------------------------

    @Test
    @DisplayName("dia futuro no mês corrente resolve para o mês anterior")
    fun `dia futuro vira mes anterior`() {
        val result = parser.parse("gastei 50 no dia 30", context)
        assertEquals(LocalDate.of(2026, 7, 30), result.draft!!.occurredAt.toLocalDate())
    }

    @Test
    @DisplayName("marcador explícito de futuro mantém a data no futuro")
    fun `marcador de futuro preserva data futura`() {
        val result = parser.parse("vou pagar 50 na próxima terça", context)
        assertEquals(LocalDate.of(2026, 9, 1), result.draft!!.occurredAt.toLocalDate())
    }

    @Test
    @DisplayName("occurredAt no passado usa hora neutra, não a hora atual")
    fun `hora neutra em datas passadas`() {
        val result = parser.parse("gastei 20 ontem", context)
        assertEquals(12, result.draft!!.occurredAt.hour)
    }

    @Test
    @DisplayName("lançamento de hoje preserva a hora atual")
    fun `hora atual em lancamento de hoje`() {
        val result = parser.parse("gastei 20 de pastel", context)
        assertEquals(14, result.draft!!.occurredAt.hour)
        assertEquals(30, result.draft!!.occurredAt.minute)
    }

    @Test
    @DisplayName("frase sem valor falha explicitamente em vez de inventar")
    fun `sem valor falha`() {
        val result = parser.parse("gastei um dinheirão no mercado", context)
        assertTrue(!result.isSuccess)
    }

    @Test
    @DisplayName("erro de transcrição é absorvido pelo fuzzy match")
    fun `fuzzy absorve erro de transcricao`() {
        val result = parser.parse("netiflix 55 reais", context)
        assertEquals(Id.ENTERTAINMENT, result.draft!!.categoryId)
    }

    @Test
    @DisplayName("descrição não contém verbo de ação nem palavra de moeda")
    fun `descricao limpa`() {
        val result = parser.parse("adicione 20 reais no dia 24 gasto com pastel", context)
        val description = result.draft!!.description.lowercase()
        assertTrue("adicione" !in description)
        assertTrue("reais" !in description)
        assertTrue("dia" !in description)
        assertTrue("pastel" in description)
    }

    @Test
    @DisplayName("frase completa e inequívoca gera confiança alta")
    fun `confianca alta em frase completa`() {
        val result = parser.parse("gastei 45,90 no mercado ontem", context)
        assertTrue(result.confidence >= 0.85f, "confiança foi ${result.confidence}")
        assertTrue(!result.needsReview)
    }

    @Test
    @DisplayName("frase sem categoria reconhecível é marcada para revisão")
    fun `frase vaga pede revisao`() {
        val result = parser.parse("50 xyzabc", context)
        assertEquals(Id.OTHER_EXPENSE, result.draft!!.categoryId)
        assertTrue(result.needsReview)
    }

    @Test
    @DisplayName("rawInput preserva o texto original para auditoria")
    fun `raw input preservado`() {
        val input = "gastei 45,90 no mercado ontem"
        assertEquals(input, parser.parse(input, context).draft!!.rawInput)
    }

    // ---- porta de qualidade --------------------------------------------------

    @Test
    @DisplayName("acurácia do corpus fica em pelo menos 92%")
    fun `acuracia minima do corpus`() {
        val cases = canonicalCases()
        val hits = cases.count { case ->
            val draft = parser.parse(case.input, context).draft
            draft != null &&
                draft.amountCents == case.cents &&
                draft.type == case.type &&
                draft.categoryId == case.categoryId &&
                draft.occurredAt.toLocalDate() == case.date
        }
        val accuracy = hits.toDouble() / cases.size
        assertTrue(accuracy >= 0.92, "acurácia foi %.1f%% (%d/%d)".format(accuracy * 100, hits, cases.size))
    }
}
