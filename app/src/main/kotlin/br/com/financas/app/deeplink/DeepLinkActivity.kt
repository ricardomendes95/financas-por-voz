package br.com.financas.app.deeplink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import br.com.financas.app.MainActivity
import br.com.financas.core.common.DeepLinks
import br.com.financas.core.model.EntrySource
import br.com.financas.feature.voice.gateway.QuickEntryGateway
import br.com.financas.feature.voice.notification.EntryNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Trampolim sem UI para a rota 7 (§5.8), a automação externa da rota 8
 * (§5.9) e a capability de App Actions da rota 2 (§5.3 — que entrega o
 * texto como extra do Intent, não como URI). `text` preenchido grava direto
 * via `QuickEntryGateway` — nunca abre tela de confirmação (regra §12). Os
 * demais casos só decidem para onde navegar dentro do `MainActivity`.
 */
@AndroidEntryPoint
class DeepLinkActivity : ComponentActivity() {

    @Inject lateinit var gateway: QuickEntryGateway
    @Inject lateinit var entryNotifier: EntryNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
        finish()
    }

    private fun handle(intent: Intent?) {
        val uri = intent?.data
        val extraText = intent?.getStringExtra(DeepLinks.PARAM_TEXT)
        val text = uri?.getQueryParameter(DeepLinks.PARAM_TEXT) ?: extraText

        if ((uri == null || uri.host == DeepLinks.HOST_ADD) && !text.isNullOrBlank()) {
            lifecycleScope.launch {
                val outcome = gateway.ingest(text, EntrySource.WIDGET)
                entryNotifier.notifyOutcome(outcome)
            }
            return
        }

        openMain(uri)
    }

    private fun openMain(uri: Uri? = null) {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (uri != null) data = uri
        }
        startActivity(mainIntent)
    }
}
