package br.com.financas.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.financas.core.common.MoneyFormatter
import br.com.financas.core.designsystem.component.AutoSizeText
import br.com.financas.core.designsystem.component.MoneyText
import br.com.financas.core.model.Insight
import br.com.financas.core.designsystem.component.TransactionRow
import br.com.financas.core.designsystem.theme.FinanceTheme
import br.com.financas.feature.dashboard.R

@Composable
fun DashboardScreen(
    onAddTransaction: () -> Unit,
    onLaunchVoiceCapture: () -> Unit,
    onEditTransaction: (String) -> Unit,
    onSeeAllTransactions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenInsightCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    DashboardContent(
        state = uiState,
        onAddTransaction = onAddTransaction,
        onLaunchVoiceCapture = onLaunchVoiceCapture,
        onEditTransaction = onEditTransaction,
        onSeeAllTransactions = onSeeAllTransactions,
        onOpenSettings = onOpenSettings,
        onOpenReports = onOpenReports,
        onOpenBudgets = onOpenBudgets,
        onOpenRecurring = onOpenRecurring,
        onOpenInsightCategory = onOpenInsightCategory,
        onConfirmSuggestion = viewModel::onConfirmSuggestion,
        onIgnoreSuggestion = viewModel::onIgnoreSuggestion,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onAddTransaction: () -> Unit,
    onLaunchVoiceCapture: () -> Unit,
    onEditTransaction: (String) -> Unit,
    onSeeAllTransactions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenInsightCategory: (String) -> Unit,
    onConfirmSuggestion: (String) -> Unit,
    onIgnoreSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.monthLabel) },
                actions = {
                    IconButton(onClick = onOpenReports) {
                        Icon(Icons.Filled.BarChart, contentDescription = stringResource(R.string.dashboard_reports))
                    }
                    IconButton(onClick = onOpenRecurring) {
                        Icon(Icons.Filled.Receipt, contentDescription = stringResource(R.string.dashboard_recurring))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.dashboard_settings))
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(onClick = onLaunchVoiceCapture) {
                    Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.dashboard_voice_capture))
                }
                Spacer(Modifier.height(16.dp))
                ExtendedFloatingActionButton(
                    onClick = onAddTransaction,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.dashboard_add_transaction)) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            // bottom maior: o Scaffold não reserva espaço pros FABs sozinho, e
            // aqui são dois empilhados (mic + Adicionar) — sem isso os
            // últimos itens da lista ficam escondidos atrás deles.
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "balance_card") { BalanceCard(state) }

            if (state.budgetLimitCents != null) {
                item(key = "budget_bar") { BudgetProgressBar(state, onOpenBudgets) }
            }

            if (state.pendingSuggestions.isNotEmpty()) {
                item(key = "pending_header") {
                    Text(
                        stringResource(R.string.dashboard_pending, state.pendingSuggestions.size),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                items(state.pendingSuggestions, key = { "pending_${it.id}" }) { suggestion ->
                    PendingSuggestionCard(suggestion, onConfirmSuggestion, onIgnoreSuggestion)
                }
            }

            if (state.insights.isNotEmpty()) {
                item(key = "insights") { InsightCarousel(state.insights, onOpenInsightCategory) }
            }

            item(key = "recent_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_recent_transactions),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            if (state.recentTransactions.isEmpty() && !state.isLoading) {
                item(key = "empty_state") { EmptyState(onAddTransaction) }
            }

            items(state.recentTransactions, key = { it.id }, contentType = { "transaction" }) { item ->
                TransactionRow(item, onClick = { onEditTransaction(item.id) })
            }

            if (state.recentTransactions.isNotEmpty()) {
                item(key = "see_all") {
                    Text(
                        text = stringResource(R.string.dashboard_see_all),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onSeeAllTransactions)
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingSuggestionCard(
    suggestion: PendingSuggestionUi,
    onConfirm: (String) -> Unit,
    onIgnore: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(suggestion.merchant, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        suggestion.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MoneyText(
                    cents = suggestion.amountCents,
                    isExpense = suggestion.isExpense,
                    color = if (suggestion.isExpense) FinanceTheme.colors.expense else FinanceTheme.colors.income,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.dashboard_confirm),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onConfirm(suggestion.id) }
                )
                Text(
                    stringResource(R.string.dashboard_ignore),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onIgnore(suggestion.id) }
                )
            }
        }
    }
}

@Composable
private fun InsightCarousel(insights: List<Insight>, onOpenCategory: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(insights, key = { it.type.name + it.message.hashCode() }) { insight ->
            val categoryId = insight.relatedCategoryId
            Card(
                modifier = Modifier
                    .width(260.dp)
                    .let { if (categoryId != null) it.clickable { onOpenCategory(categoryId) } else it },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = insight.message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun BudgetProgressBar(state: DashboardUiState, onClick: () -> Unit) {
    val limit = state.budgetLimitCents ?: return
    val progress = (state.expenseCents.toFloat() / limit).coerceIn(0f, 1.5f)
    val isOverPace = progress > state.budgetExpectedProgress + 0.05f
    val barColor = if (isOverPace) FinanceTheme.colors.warning else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.dashboard_budget), style = MaterialTheme.typography.labelMedium)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                drawRoundRect(color = trackColor, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                drawRoundRect(
                    color = barColor,
                    size = androidx.compose.ui.geometry.Size(size.width * progress.coerceAtMost(1f), size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
                val markerX = size.width * state.budgetExpectedProgress
                drawLine(
                    color = markerColor,
                    start = androidx.compose.ui.geometry.Offset(markerX, 0f),
                    end = androidx.compose.ui.geometry.Offset(markerX, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

/** TalkBack lê o sinal por extenso — nunca só a cor (§10.3). */
private fun spokenBalance(cents: Long): String {
    val prefix = if (cents < 0) "Saldo negativo de" else "Saldo de"
    return "$prefix ${MoneyFormatter.format(kotlin.math.abs(cents))}"
}

@Composable
private fun BalanceCard(state: DashboardUiState) {
    val isNegative = state.balanceCents < 0
    // Anima do valor anterior até o novo em ~600ms — nunca "pula" (§10.3).
    val animatedCents by androidx.compose.animation.core.animateIntAsState(
        targetValue = state.balanceCents.toInt(),
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f),
        label = "balance"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.dashboard_month_balance),
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(4.dp))
            AutoSizeText(
                text = MoneyFormatter.format(animatedCents.toLong()),
                style = MaterialTheme.typography.displayLarge,
                color = if (isNegative) FinanceTheme.colors.expense else FinanceTheme.colors.income,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = spokenBalance(state.balanceCents)
                    }
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MoneyText(
                    cents = state.incomeCents,
                    isExpense = false,
                    color = FinanceTheme.colors.income,
                    style = MaterialTheme.typography.bodyMedium
                )
                MoneyText(
                    cents = state.expenseCents,
                    isExpense = true,
                    color = FinanceTheme.colors.expense,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onAddTransaction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.dashboard_empty_state),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.dashboard_add_transaction),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = onAddTransaction)
                .padding(8.dp)
        )
    }
}
