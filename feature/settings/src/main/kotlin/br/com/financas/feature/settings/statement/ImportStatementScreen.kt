package br.com.financas.feature.settings.statement

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.financas.core.common.MoneyFormatter
import br.com.financas.core.common.RelativeDateFormatter
import br.com.financas.core.designsystem.theme.FinanceTheme
import br.com.financas.core.model.StatementPreviewItem
import br.com.financas.feature.settings.R
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportStatementScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatementImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val content = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            }
        }.getOrNull()
        if (content != null) viewModel.onFileSelected(content) else viewModel.onReadFailed()
    }
    val openFilePicker = { filePickerLauncher.launch(arrayOf("text/*", "text/csv", "*/*")) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_statement_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is StatementImportUiState.Idle -> IdleContent(openFilePicker)
                is StatementImportUiState.Loading -> LoadingContent()
                is StatementImportUiState.Error -> ErrorContent(state.message, openFilePicker)
                is StatementImportUiState.Preview -> PreviewContent(
                    state = state,
                    onToggle = viewModel::onToggleSelection,
                    onConfirm = viewModel::onConfirmImport
                )
                is StatementImportUiState.Done -> DoneContent(state.importedCount, onBack)
            }
        }
    }
}

@Composable
private fun IdleContent(onOpenFilePicker: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.import_statement_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onOpenFilePicker) {
            Text(stringResource(R.string.import_statement_pick_file))
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.import_statement_pick_file))
        }
    }
}

@Composable
private fun DoneContent(importedCount: Int, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.import_statement_done, importedCount),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text(stringResource(R.string.import_statement_done_confirm))
        }
    }
}

@Composable
private fun PreviewContent(
    state: StatementImportUiState.Preview,
    onToggle: (String) -> Unit,
    onConfirm: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.import_statement_selected_count, state.selectedCount, state.items.size),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(state.items, key = { it.entry.externalId }) { item ->
                StatementRow(
                    item = item,
                    selected = item.entry.externalId in state.selectedExternalIds,
                    onToggle = { onToggle(item.entry.externalId) }
                )
            }
        }
        Button(
            onClick = onConfirm,
            enabled = state.selectedCount > 0,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(stringResource(R.string.import_statement_confirm, state.selectedCount))
        }
    }
}

@Composable
private fun StatementRow(item: StatementPreviewItem, selected: Boolean, onToggle: () -> Unit) {
    val isExpense = item.entry.type.name == "EXPENSE"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (item.alreadyImported) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(item.entry.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Text(
                    "${item.categoryName} · ${RelativeDateFormatter.format(item.entry.occurredAt)}" +
                        if (item.alreadyImported) " · ${stringResource(R.string.import_statement_already_imported)}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                MoneyFormatter.format(if (isExpense) -item.entry.amountCents else item.entry.amountCents),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isExpense) FinanceTheme.colors.expense else FinanceTheme.colors.income
            )
        }
    }
}
