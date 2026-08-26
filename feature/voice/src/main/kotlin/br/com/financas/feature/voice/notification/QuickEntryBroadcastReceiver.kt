package br.com.financas.feature.voice.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import br.com.financas.core.common.YearMonthUtils
import br.com.financas.core.data.repository.TransactionRepository
import br.com.financas.core.model.EntrySource
import br.com.financas.feature.voice.gateway.QuickEntryGateway
import br.com.financas.feature.voice.notification.QuickEntryActions.ACTION_REMOTE_INPUT
import br.com.financas.feature.voice.notification.QuickEntryActions.ACTION_UNDO
import br.com.financas.feature.voice.notification.QuickEntryActions.EXTRA_TRANSACTION_ID
import br.com.financas.feature.voice.notification.QuickEntryActions.REMOTE_INPUT_KEY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Recebe as ações que não abrem tela: Desfazer (notificação de confirmação)
 * e o texto ditado/digitado no `RemoteInput` da notificação persistente
 * (rota 6) — a rota mais resiliente, funciona sem nenhum assistente.
 */
@AndroidEntryPoint
class QuickEntryBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var gateway: QuickEntryGateway
    @Inject lateinit var transactionRepository: TransactionRepository
    @Inject lateinit var entryNotifier: EntryNotifier
    @Inject lateinit var clock: Clock

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_UNDO -> handleUndo(intent)
                    ACTION_REMOTE_INPUT -> handleRemoteInput(intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleUndo(intent: Intent) {
        val id = intent.getStringExtra(EXTRA_TRANSACTION_ID) ?: return
        transactionRepository.delete(id)
        entryNotifier.cancelConfirmation()
        refreshPersistent()
    }

    private suspend fun handleRemoteInput(intent: Intent) {
        val text = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(REMOTE_INPUT_KEY)
            ?.toString()
            ?.trim()
        if (text.isNullOrBlank()) return

        val outcome = gateway.ingest(text, EntrySource.NOTIFICATION)
        entryNotifier.notifyOutcome(outcome)
        refreshPersistent()
    }

    private suspend fun refreshPersistent() {
        val yearMonth = YearMonthUtils.currentYearMonth(clock.zone)
        val summary = transactionRepository.observeMonthlySummary(yearMonth).first()
        val todaySpent = transactionRepository.observeTodaySpent().first()
        entryNotifier.updatePersistent(summary.balanceCents, todaySpent)
    }
}
