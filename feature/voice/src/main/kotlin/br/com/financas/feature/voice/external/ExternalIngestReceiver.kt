package br.com.financas.feature.voice.external

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.financas.core.model.EntrySource
import br.com.financas.feature.voice.gateway.QuickEntryGateway
import br.com.financas.feature.voice.notification.EntryNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Rota 8 (§5.9): automação externa (Tasker/MacroDroid) via broadcast. */
@AndroidEntryPoint
class ExternalIngestReceiver : BroadcastReceiver() {

    @Inject lateinit var gateway: QuickEntryGateway
    @Inject lateinit var entryNotifier: EntryNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra(EXTRA_TEXT)?.trim()
        if (text.isNullOrBlank()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val outcome = gateway.ingest(text, EntrySource.WIDGET)
                entryNotifier.notifyOutcome(outcome)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_INGEST = "br.com.financas.ACTION_INGEST"
        const val PERMISSION_INGEST = "br.com.financas.permission.INGEST"
        const val EXTRA_TEXT = "extra_text"
    }
}
