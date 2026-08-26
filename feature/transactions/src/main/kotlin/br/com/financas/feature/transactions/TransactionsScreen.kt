package br.com.financas.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.financas.core.common.MoneyFormatter
import br.com.financas.core.designsystem.component.TransactionRow
import br.com.financas.core.designsystem.theme.FinanceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transactions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.transactions_back))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.transactions_add)) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "month_selector") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = viewModel::onPreviousMonth) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.transactions_previous_month))
                    }
                    Text(uiState.monthLabel, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = viewModel::onNextMonth) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.transactions_next_month))
                    }
                }
            }

            item(key = "type_filter") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TypeFilter.entries.toList(), key = { it.name }) { filter ->
                        FilterChip(
                            selected = uiState.typeFilter == filter,
                            onClick = { viewModel.onTypeFilterChange(filter) },
                            label = { Text(filter.label()) }
                        )
                    }
                }
            }

            item(key = "month_total") {
                Text(
                    text = MoneyFormatter.format(uiState.totalCents),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (uiState.totalCents < 0) FinanceTheme.colors.expense else FinanceTheme.colors.income
                )
            }

            uiState.groups.forEach { group ->
                stickyHeader(key = group.header) {
                    Surface(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = group.header,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                items(group.items, key = { it.id }, contentType = { "transaction" }) { item ->
                    TransactionRow(item, onClick = { onEditTransaction(item.id) })
                }
            }
        }
    }
}

private fun TypeFilter.label(): String = when (this) {
    TypeFilter.ALL -> "Todos"
    TypeFilter.EXPENSE -> "Despesas"
    TypeFilter.INCOME -> "Receitas"
}
