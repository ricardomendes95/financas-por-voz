package br.com.financas.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.financas.core.data.backup.AutoBackupPreferences
import br.com.financas.core.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

sealed interface BackupEvent {
    data object ExportSucceeded : BackupEvent
    data object ExportFailed : BackupEvent
    data object ImportSucceeded : BackupEvent
    data object ImportFailed : BackupEvent
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val autoBackupPreferences: AutoBackupPreferences
) : ViewModel() {

    private val events = Channel<BackupEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val autoBackupEnabled: StateFlow<Boolean> = autoBackupPreferences.observeEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun onAutoBackupToggle(enabled: Boolean) {
        viewModelScope.launch { autoBackupPreferences.setEnabled(enabled) }
    }

    fun onExport(openOutput: () -> OutputStream?) {
        viewModelScope.launch(Dispatchers.IO) {
            val output = openOutput()
            val success = output != null && output.use { backupRepository.exportTo(it) }
            events.send(if (success) BackupEvent.ExportSucceeded else BackupEvent.ExportFailed)
        }
    }

    fun onImport(openInput: () -> InputStream?) {
        viewModelScope.launch(Dispatchers.IO) {
            val input = openInput()
            val success = input != null && input.use { backupRepository.importFrom(it) }
            events.send(if (success) BackupEvent.ImportSucceeded else BackupEvent.ImportFailed)
        }
    }
}
