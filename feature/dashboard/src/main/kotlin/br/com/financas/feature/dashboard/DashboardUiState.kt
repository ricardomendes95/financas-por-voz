package br.com.financas.feature.dashboard

import br.com.financas.core.model.Insight
import br.com.financas.core.model.TransactionListItem

data class DashboardUiState(
    val yearMonth: Int = 0,
    val monthLabel: String = "",
    /** Saldo total acumulado (carry-over de todos os meses + este) — é o número "hero" do card. */
    val accumulatedBalanceCents: Long = 0,
    /** Resultado só deste mês (entrou - saiu) — não é mais o número principal, mas segue exibido. */
    val balanceCents: Long = 0,
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    /** Resultado (entrou - saiu) do mês anterior — explica quanto do saldo total veio de lá: sobra soma, déficit subtrai. */
    val previousMonthBalanceCents: Long = 0,
    val recentTransactions: List<TransactionListItem> = emptyList(),
    /** No máximo 3 visíveis no carrossel (§6.1) — o engine já devolve ranqueado por impacto. */
    val insights: List<Insight> = emptyList(),
    val budgetLimitCents: Long? = null,
    /** Fração 0..1 de quanto do mês já passou — marcador de ritmo esperado na barra. */
    val budgetExpectedProgress: Float = 0f,
    /** Sugestões de notificação bancária (§8) — nunca viram lançamento sem confirmação explícita. */
    val pendingSuggestions: List<PendingSuggestionUi> = emptyList(),
    val isLoading: Boolean = true
)

data class PendingSuggestionUi(
    val id: String,
    val amountCents: Long,
    val isExpense: Boolean,
    val merchant: String,
    val categoryName: String
)
