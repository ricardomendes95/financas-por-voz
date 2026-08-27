package br.com.financas.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.financas.core.common.DeepLinks
import br.com.financas.core.data.tour.TourStep
import br.com.financas.core.designsystem.tour.tourTarget
import java.time.LocalDate

// Índice de cada Card dentro do LazyColumn abaixo — usado só para rolar até o card
// certo quando o tour guiado chega num passo desta tela. Se a ordem dos `item { }`
// mudar, estes valores precisam ser atualizados junto.
private const val SETTINGS_CATEGORIES_ITEM_INDEX = 2
private const val SETTINGS_IMPORT_ITEM_INDEX = 3
private const val SETTINGS_BANK_ITEM_INDEX = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenMonthClosing: () -> Unit,
    onOpenBankAllowlist: () -> Unit,
    onOpenImportStatement: () -> Unit,
    onOpenCategories: () -> Unit,
    onReplayTour: () -> Unit,
    tourStep: TourStep?,
    modifier: Modifier = Modifier,
    backupViewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val autoBackupEnabled by backupViewModel.autoBackupEnabled.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(tourStep) {
        val targetIndex = when (tourStep?.targetId) {
            "settings_categories_card" -> SETTINGS_CATEGORIES_ITEM_INDEX
            "settings_import_card" -> SETTINGS_IMPORT_ITEM_INDEX
            "settings_bank_card" -> SETTINGS_BANK_ITEM_INDEX
            else -> null
        }
        targetIndex?.let { listState.animateScrollToItem(it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            backupViewModel.onExport { context.contentResolver.openOutputStream(uri) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImportUri = uri
    }

    val exportSuccessMessage = stringResource(R.string.settings_backup_export_success)
    val exportFailedMessage = stringResource(R.string.settings_backup_export_failed)
    val importFailedMessage = stringResource(R.string.settings_backup_import_failed)
    val importSuccessMessage = stringResource(R.string.settings_backup_import_success)

    LaunchedEffect(Unit) {
        backupViewModel.eventFlow.collect { event ->
            when (event) {
                BackupEvent.ExportSucceeded -> Toast.makeText(context, exportSuccessMessage, Toast.LENGTH_SHORT).show()
                BackupEvent.ExportFailed -> Toast.makeText(context, exportFailedMessage, Toast.LENGTH_SHORT).show()
                BackupEvent.ImportFailed -> Toast.makeText(context, importFailedMessage, Toast.LENGTH_SHORT).show()
                BackupEvent.ImportSucceeded -> {
                    Toast.makeText(context, importSuccessMessage, Toast.LENGTH_LONG).show()
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    val restartIntent = Intent.makeRestartActivityTask(launchIntent?.component)
                    context.startActivity(restartIntent)
                    Runtime.getRuntime().exit(0)
                }
            }
        }
    }

    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.settings_backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.settings_backup_import_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingImportUri
                    pendingImportUri = null
                    if (uri != null) {
                        backupViewModel.onImport { context.contentResolver.openInputStream(uri) }
                    }
                }) { Text(stringResource(R.string.settings_backup_import_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text(stringResource(R.string.settings_backup_import_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_voice_section), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.settings_assistant_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }) {
                            Text(stringResource(R.string.settings_assistant_button))
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_shortcut_section), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.settings_shortcut_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinks.capture()))
                            )
                        }) {
                            Text(stringResource(R.string.settings_shortcut_test))
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().tourTarget("settings_categories_card")) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_categories_section), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.settings_categories_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onOpenCategories) {
                            Text(stringResource(R.string.settings_categories_button))
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().tourTarget("settings_import_card")) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_import_section), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.settings_import_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onOpenImportStatement) {
                            Text(stringResource(R.string.settings_import_button))
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_backup_section), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.settings_backup_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { exportLauncher.launch("financas-backup-${LocalDate.now()}.db") }) {
                            Text(stringResource(R.string.settings_backup_export))
                        }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                            Text(stringResource(R.string.settings_backup_import))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.settings_auto_backup_title), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    stringResource(R.string.settings_auto_backup_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = autoBackupEnabled, onCheckedChange = backupViewModel::onAutoBackupToggle)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_closing_section), style = MaterialTheme.typography.titleLarge)
                        OutlinedButton(onClick = onOpenMonthClosing) {
                            Text(stringResource(R.string.settings_closing_button))
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth().tourTarget("settings_bank_card")) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_bank_section), style = MaterialTheme.typography.titleLarge)
                        OutlinedButton(onClick = onOpenBankAllowlist) {
                            Text(stringResource(R.string.settings_bank_button))
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_tour_section), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.settings_tour_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onReplayTour) {
                            Text(stringResource(R.string.settings_tour_button))
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.settings_about_section), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.settings_about_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
