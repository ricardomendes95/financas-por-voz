package br.com.financas.feature.voice.external

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import br.com.financas.core.model.EntrySource
import br.com.financas.feature.voice.gateway.QuickEntryGateway
import br.com.financas.feature.voice.notification.EntryNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Rota 8 (§5.9): selecionar texto em qualquer app → menu "Lançar no
 * Finanças". Sem UI própria — grava direto e fecha.
 */
@AndroidEntryPoint
class ProcessTextActivity : ComponentActivity() {

    @Inject lateinit var gateway: QuickEntryGateway
    @Inject lateinit var entryNotifier: EntryNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.trim()
        if (text.isNullOrBlank()) {
            finish()
            return
        }
        lifecycleScope.launch {
            val outcome = gateway.ingest(text, EntrySource.WIDGET)
            entryNotifier.notifyOutcome(outcome)
            finish()
        }
    }
}
