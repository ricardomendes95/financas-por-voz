package br.com.financas.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.financas.core.common.MoneyFormatter
import br.com.financas.core.common.RelativeDateFormatter
import br.com.financas.core.model.PaymentMethod
import br.com.financas.core.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                AddEditTransactionEvent.Saved -> onSaved()
                AddEditTransactionEvent.Deleted -> onSaved()
            }
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // TextFieldValue (não só String) pra controlar o cursor explicitamente:
    // sem isso, cada vez que a máscara reescreve o texto o Compose tenta
    // adivinhar onde recolocar o cursor e ele "pula" pro meio do valor,
    // fazendo os próximos dígitos entrarem no lugar errado. Resincroniza
    // com o ViewModel só quando o texto muda por fonte externa (carregar
    // uma edição) — quando a mudança veio do próprio campo, os dois já
    // estão iguais e não há nada a fazer.
    var amountField by remember { mutableStateOf(TextFieldValue(uiState.amountText)) }
    LaunchedEffect(uiState.amountText) {
        if (uiState.amountText != amountField.text) {
            amountField = TextFieldValue(uiState.amountText, TextRange(uiState.amountText.length))
        }
    }

    val categoriesForType by remember(uiState.allCategories, uiState.type) {
        derivedStateOf {
            uiState.allCategories.filter { it.type == null || it.type == uiState.type }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditing) R.string.edit_transaction_title else R.string.add_transaction_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.transactions_back))
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.edit_transaction_delete))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.type == TransactionType.EXPENSE,
                    onClick = { viewModel.onTypeChange(TransactionType.EXPENSE) },
                    shape = MaterialTheme.shapes.small
                ) { Text(stringResource(R.string.add_transaction_expense)) }
                SegmentedButton(
                    selected = uiState.type == TransactionType.INCOME,
                    onClick = { viewModel.onTypeChange(TransactionType.INCOME) },
                    shape = MaterialTheme.shapes.small
                ) { Text(stringResource(R.string.add_transaction_income)) }
            }

            OutlinedTextField(
                value = amountField,
                // Máscara tipo calculadora: cada dígito digitado empurra os
                // centavos da direita pra esquerda ("2" → 0,02 → "0" → 0,20 →
                // "5" → 2,05), sem depender do usuário digitar vírgula/ponto.
                onValueChange = { newValue ->
                    val masked = maskAmountInput(newValue.text)
                    amountField = TextFieldValue(masked, TextRange(masked.length))
                    viewModel.onAmountChange(masked)
                },
                label = { Text(stringResource(R.string.add_transaction_amount)) },
                isError = uiState.amountInvalid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.add_transaction_description)) },
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.add_transaction_category), style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categoriesForType, key = { it.id }) { category ->
                    FilterChip(
                        selected = uiState.selectedCategoryId == category.id,
                        onClick = { viewModel.onCategorySelect(category.id) },
                        label = { Text(category.name) }
                    )
                }
            }

            Text(stringResource(R.string.add_transaction_payment), style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PaymentMethod.entries) { method ->
                    FilterChip(
                        selected = uiState.paymentMethod == method,
                        onClick = { viewModel.onPaymentMethodChange(method) },
                        label = { Text(method.label()) }
                    )
                }
            }

            TextButton(onClick = { showDatePicker = true }) {
                Text(stringResource(R.string.add_transaction_date, RelativeDateFormatter.format(uiState.occurredAt)))
            }

            Button(
                onClick = viewModel::onSave,
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add_transaction_save))
            }
        }
    }

    if (showDatePicker) {
        // DatePickerState sempre trabalha em UTC (meia-noite do dia, fuso zero),
        // independente do fuso do aparelho — sem essa conversão de ida e volta,
        // um dia escolhido no fuso local (ex.: UTC-3) chega e sai deslocado em
        // um dia (2 de agosto vira meia-noite UTC do dia 1 no horário local).
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.occurredAt.toUtcMidnightMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it.fromUtcMidnightMillisToLocalMidnight()) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.add_transaction_date_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.add_transaction_date_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.edit_transaction_delete_confirm_title)) },
            text = { Text(stringResource(R.string.edit_transaction_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.onDelete()
                }) { Text(stringResource(R.string.edit_transaction_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.edit_transaction_delete_cancel))
                }
            }
        )
    }
}

private fun PaymentMethod.label(): String = when (this) {
    PaymentMethod.PIX -> "Pix"
    PaymentMethod.CREDIT -> "Crédito"
    PaymentMethod.DEBIT -> "Débito"
    PaymentMethod.CASH -> "Dinheiro"
    PaymentMethod.BOLETO -> "Boleto"
    PaymentMethod.TRANSFER -> "Transferência"
}

/** Trata todos os dígitos do texto como centavos e reformata — dígitos entram pela direita, como numa calculadora. */
private fun maskAmountInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.trimStart('0')
    if (digits.isBlank()) return ""
    val cents = digits.toLongOrNull() ?: return ""
    return MoneyFormatter.formatPlain(cents)
}

/** Mesma data (ano/mês/dia) do horário local, representada à meia-noite UTC — formato que o DatePicker do Compose espera. */
private fun Long.toUtcMidnightMillis(): Long {
    val localDate = java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    return localDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
}

/** Inverso de [toUtcMidnightMillis]: extrai a data do valor UTC do DatePicker e a reconstrói à meia-noite no fuso local. */
private fun Long.fromUtcMidnightMillisToLocalMidnight(): Long {
    val localDate = java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneOffset.UTC).toLocalDate()
    return localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
}
