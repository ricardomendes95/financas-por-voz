package br.com.financas.feature.budgets

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.financas.core.common.MoneyFormatter
import br.com.financas.core.designsystem.component.CategoryIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.budgets_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::onCopyFromPreviousMonth) {
                        Text(stringResource(R.string.budgets_copy_previous))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.budgets_general), style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = uiState.generalLimitText,
                            onValueChange = viewModel::onGeneralLimitChange,
                            label = { Text(stringResource(R.string.budgets_limit)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = uiState.rollover, onCheckedChange = viewModel::onRolloverChange)
                            Text(stringResource(R.string.budgets_rollover))
                        }
                        Text(
                            stringResource(R.string.budgets_spent_so_far, MoneyFormatter.format(uiState.generalSpentCents)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(uiState.rows, key = { it.categoryId }) { row ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(CategoryIcons.resolve(row.icon), contentDescription = null)
                            Text(row.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(
                            value = row.limitText,
                            onValueChange = { viewModel.onCategoryLimitChange(row.categoryId, it) },
                            label = { Text(stringResource(R.string.budgets_limit)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            stringResource(R.string.budgets_spent_so_far, MoneyFormatter.format(row.spentCents)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        row.suggestedCents?.let { suggested ->
                            OutlinedButton(onClick = { viewModel.onApplySuggestion(row.categoryId) }) {
                                Text(stringResource(R.string.budgets_suggestion, MoneyFormatter.format(suggested)))
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = viewModel::onSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.budgets_save))
                }
            }
        }
    }
}
