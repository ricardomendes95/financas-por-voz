package br.com.financas.feature.reports.closing

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.financas.core.common.MoneyFormatter
import br.com.financas.core.designsystem.theme.FinanceTheme
import br.com.financas.feature.reports.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthClosingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MonthClosingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.closing_title, uiState.monthLabel)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            LabeledValue(stringResource(R.string.closing_income), MoneyFormatter.format(uiState.incomeCents), FinanceTheme.colors.income)
                            LabeledValue(stringResource(R.string.closing_expense), MoneyFormatter.format(uiState.expenseCents), FinanceTheme.colors.expense)
                        }
                        Text(
                            stringResource(R.string.closing_balance, MoneyFormatter.format(uiState.balanceCents)),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            stringResource(
                                R.string.closing_accumulated_balance,
                                MoneyFormatter.format(uiState.accumulatedBalanceCents)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.accumulatedBalanceCents < 0) FinanceTheme.colors.expense else FinanceTheme.colors.income
                        )
                        Text(
                            stringResource(R.string.closing_savings_rate, uiState.savingsRatePercent),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        uiState.vsLastMonthPercent?.let { delta ->
                            Text(
                                stringResource(R.string.closing_vs_last_month, if (delta >= 0) "+$delta" else "$delta"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.closing_top_categories), style = MaterialTheme.typography.titleMedium)
            }
            items(uiState.topCategories, key = { it.categoryId }) { category ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(category.name)
                    Text(MoneyFormatter.format(category.totalCents))
                }
            }

            if (uiState.opportunities.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.closing_opportunities), style = MaterialTheme.typography.titleMedium)
                }
                items(uiState.opportunities, key = { it.type.name }) { insight ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(insight.message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_TEXT, uiState.csvContent)
                            putExtra(Intent.EXTRA_SUBJECT, "Exportação Finanças — ${uiState.monthLabel}")
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.closing_export_csv))
                }
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = color)
    }
}
