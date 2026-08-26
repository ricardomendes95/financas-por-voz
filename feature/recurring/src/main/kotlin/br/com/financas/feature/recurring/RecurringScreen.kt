package br.com.financas.feature.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.financas.core.common.MoneyFormatter
import br.com.financas.core.common.RelativeDateFormatter
import br.com.financas.core.designsystem.component.CategoryIcons
import br.com.financas.core.designsystem.theme.FinanceTheme
import br.com.financas.core.model.Category
import br.com.financas.core.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    onBack: () -> Unit,
    onEditPaidTransaction: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var confirmingRow by remember { mutableStateOf<RecurringRowUi?>(null) }
    var linkingRow by remember { mutableStateOf<RecurringRowUi?>(null) }
    var deletingRow by remember { mutableStateOf<RecurringRowUi?>(null) }
    val linkCandidates by viewModel.linkCandidates.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recurring_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.recurring_new)) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            // bottom maior que o resto: o Scaffold não reserva espaço pro FAB
            // sozinho, então sem isso o último item da lista fica escondido
            // atrás dele.
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "month_selector") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = viewModel::onPreviousMonth) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Mês anterior")
                    }
                    Text(uiState.monthLabel, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = viewModel::onNextMonth) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Próximo mês")
                    }
                }
            }

            if (uiState.rows.isEmpty() && !uiState.isLoading) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.recurring_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val pending = uiState.rows.filter { !it.isPaid }
            val paid = uiState.rows.filter { it.isPaid }

            if (pending.isEmpty() && paid.isNotEmpty()) {
                item(key = "all_paid") {
                    Text(
                        stringResource(R.string.recurring_all_paid),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(pending, key = { it.ruleId }) { row ->
                RecurringRuleCard(
                    row = row,
                    onClick = { confirmingRow = row },
                    onDelete = { deletingRow = row }
                )
            }

            if (paid.isNotEmpty()) {
                item(key = "paid_header") {
                    Text(
                        stringResource(R.string.recurring_paid_header),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(paid, key = { it.ruleId }) { row ->
                    RecurringRuleCard(
                        row = row,
                        onClick = { row.paidTransactionId?.let(onEditPaidTransaction) },
                        onDelete = { deletingRow = row }
                    )
                }
            }
        }
    }

    confirmingRow?.let { row ->
        ConfirmPaymentDialog(
            row = row,
            onDismiss = { confirmingRow = null },
            onConfirm = { amountCents ->
                viewModel.onConfirmPayment(row.ruleId, amountCents)
                confirmingRow = null
            },
            onRequestLink = {
                confirmingRow = null
                viewModel.onLoadLinkCandidates(row.type)
                linkingRow = row
            }
        )
    }

    linkingRow?.let { row ->
        LinkExistingTransactionDialog(
            row = row,
            candidates = linkCandidates,
            onDismiss = {
                linkingRow = null
                viewModel.onClearLinkCandidates()
            },
            onSelect = { transactionId ->
                viewModel.onLinkExistingPayment(row.ruleId, transactionId)
                linkingRow = null
            }
        )
    }

    deletingRow?.let { row ->
        AlertDialog(
            onDismissRequest = { deletingRow = null },
            title = { Text(stringResource(R.string.recurring_delete_title)) },
            text = { Text(stringResource(R.string.recurring_delete_message, row.description)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeleteRule(row.ruleId)
                    deletingRow = null
                }) { Text(stringResource(R.string.recurring_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingRow = null }) { Text(stringResource(R.string.recurring_delete_cancel)) }
            }
        )
    }

    if (showCreateDialog) {
        CreateRecurringRuleDialog(
            categories = uiState.allCategories,
            onDismiss = { showCreateDialog = false },
            onConfirm = { description, amountCents, categoryId, type, dayOfMonth ->
                viewModel.onCreateRule(description, amountCents, categoryId, type, dayOfMonth)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun RecurringRuleCard(row: RecurringRowUi, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = Color(row.categoryColorArgb), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(CategoryIcons.resolve(row.categoryIcon), contentDescription = null, tint = Color.White)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(row.description, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.recurring_due_day, row.dayOfMonth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    MoneyFormatter.format(row.paidAmountCents ?: row.defaultAmountCents),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            RowStatusChip(row)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.recurring_delete))
            }
        }
    }
}

@Composable
private fun RowStatusChip(row: RecurringRowUi) {
    val (label, containerColor) = when {
        row.isPaid -> stringResource(R.string.recurring_status_paid) to FinanceTheme.colors.income
        row.isOverdue -> stringResource(R.string.recurring_status_overdue) to FinanceTheme.colors.expense
        else -> stringResource(R.string.recurring_status_pending) to MaterialTheme.colorScheme.surfaceVariant
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = containerColor.copy(alpha = 0.18f))
    )
}

@Composable
private fun ConfirmPaymentDialog(row: RecurringRowUi, onDismiss: () -> Unit, onConfirm: (Long) -> Unit, onRequestLink: () -> Unit) {
    var amountField by remember {
        mutableStateOf(TextFieldValue(MoneyFormatter.formatPlain(row.defaultAmountCents)))
    }
    val amountCents = remember(amountField.text) { MoneyFormatter.parseToCents(amountField.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recurring_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(row.description, style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = amountField,
                    onValueChange = { newValue ->
                        val masked = maskAmountInput(newValue.text)
                        amountField = TextFieldValue(masked, TextRange(masked.length))
                    },
                    label = { Text(stringResource(R.string.recurring_confirm_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = onRequestLink) {
                    Text(stringResource(R.string.recurring_confirm_link_existing))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { amountCents?.let(onConfirm) }, enabled = amountCents != null && amountCents > 0) {
                Text(stringResource(R.string.recurring_confirm_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.recurring_confirm_cancel)) }
        }
    )
}

@Composable
private fun LinkExistingTransactionDialog(
    row: RecurringRowUi,
    candidates: List<LinkCandidateUi>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    // A busca filtra só entre os candidatos já carregados do mês em questão — associar
    // um lançamento de outro mês não faz sentido (a regra é "pago no mês X ou não").
    val filtered by remember(candidates, query) {
        derivedStateOf {
            if (query.isBlank()) candidates else candidates.filter { it.description.contains(query, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recurring_link_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.recurring_link_subtitle, row.description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (candidates.isNotEmpty()) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.recurring_link_search)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (candidates.isEmpty()) {
                    Text(
                        stringResource(R.string.recurring_link_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (filtered.isEmpty()) {
                    Text(
                        stringResource(R.string.recurring_link_no_match),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.transactionId }) { candidate ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { onSelect(candidate.transactionId) },
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(candidate.description, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            RelativeDateFormatter.format(candidate.occurredAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(MoneyFormatter.format(candidate.amountCents), style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.recurring_link_cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRecurringRuleDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (description: String, amountCents: Long, categoryId: String, type: TransactionType, dayOfMonth: Int) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountField by remember { mutableStateOf(TextFieldValue("")) }
    var dayText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    val categoriesForType by remember(categories, type) {
        derivedStateOf { categories.filter { it.type == null || it.type == type } }
    }
    val amountCents = remember(amountField.text) { MoneyFormatter.parseToCents(amountField.text) }
    val dayOfMonth = dayText.toIntOrNull()
    val canSave = description.isNotBlank() &&
        amountCents != null && amountCents > 0 &&
        selectedCategoryId != null &&
        dayOfMonth != null && dayOfMonth in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.recurring_new)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.recurring_create_description)) },
                    modifier = Modifier.fillMaxWidth()
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE; selectedCategoryId = null },
                        shape = MaterialTheme.shapes.small
                    ) { Text(stringResource(R.string.recurring_create_type_expense)) }
                    SegmentedButton(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME; selectedCategoryId = null },
                        shape = MaterialTheme.shapes.small
                    ) { Text(stringResource(R.string.recurring_create_type_income)) }
                }

                OutlinedTextField(
                    value = amountField,
                    onValueChange = { newValue ->
                        val masked = maskAmountInput(newValue.text)
                        amountField = TextFieldValue(masked, TextRange(masked.length))
                    },
                    label = { Text(stringResource(R.string.recurring_create_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dayText,
                    onValueChange = { newValue -> dayText = newValue.filter { it.isDigit() }.take(2) },
                    label = { Text(stringResource(R.string.recurring_create_day)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(R.string.recurring_create_category), style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categoriesForType, key = { it.id }) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { selectedCategoryId = category.id },
                            label = { Text(category.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(description, amountCents!!, selectedCategoryId!!, type, dayOfMonth!!)
                },
                enabled = canSave
            ) { Text(stringResource(R.string.recurring_create_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.recurring_create_cancel)) }
        }
    )
}

/** Trata todos os dígitos do texto como centavos e reformata — dígitos entram pela direita, como numa calculadora. */
private fun maskAmountInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.trimStart('0')
    if (digits.isBlank()) return ""
    val cents = digits.toLongOrNull() ?: return ""
    return MoneyFormatter.formatPlain(cents)
}
