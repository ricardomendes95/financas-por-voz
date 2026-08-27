package br.com.financas.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            // bottom maior que o resto: o Scaffold não reserva espaço pro FAB
            // sozinho, então sem isso o último item da lista fica escondido
            // atrás dele.
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "search_field") {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    label = { Text(stringResource(R.string.transactions_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.isSearching) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.transactions_search_clear))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!uiState.isSearching) {
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

            item(key = "category_filter") {
                var expanded by remember { mutableStateOf(false) }
                // Com "Todos" selecionado, uiState.typeFilter.type é null — nesse caso não filtra
                // por tipo (mostra todas), em vez de só as categorias sem tipo definido.
                val filterType = uiState.typeFilter.type
                val categoriesForType = uiState.allCategories.filter {
                    filterType == null || it.type == null || it.type == filterType
                }
                val selectedName = categoriesForType.firstOrNull { it.id == uiState.categoryFilter }?.name
                    ?: stringResource(R.string.transactions_category_all)

                Box {
                    FilterChip(
                        selected = uiState.categoryFilter != null,
                        onClick = { expanded = true },
                        label = { Text(selectedName) },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.transactions_category_all)) },
                            onClick = {
                                viewModel.onCategoryFilterChange(null)
                                expanded = false
                            }
                        )
                        categoriesForType.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    viewModel.onCategoryFilterChange(category.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (!uiState.isSearching) {
                item(key = "month_total") {
                    Text(
                        text = MoneyFormatter.format(uiState.totalCents),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (uiState.totalCents < 0) FinanceTheme.colors.expense else FinanceTheme.colors.income
                    )
                }
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
