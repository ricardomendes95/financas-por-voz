package br.com.financas.integration.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import br.com.financas.core.data.repository.PendingSuggestionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * §8: lê só os pacotes que o usuário marcou na allowlist, nunca lança
 * automaticamente — sempre cria uma sugestão pendente. Nada sai do
 * dispositivo (regra §8.4.4): sem rede, sem `Log.*` do texto da notificação
 * em build de release.
 */
@AndroidEntryPoint
class BankNotificationListener : NotificationListenerService() {

    @Inject lateinit var allowlistPreferences: BankAllowlistPreferences
    @Inject lateinit var suggestionRepository: PendingSuggestionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        scope.launch {
            val allowed = allowlistPreferences.observeEnabledPackages().first()
            if (sbn.packageName !in allowed) return@launch

            val text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return@launch
            val result = BankMessageParser.parse(text) ?: return@launch
            val merchant = MerchantNormalizer.normalize(result.merchantRaw)

            suggestionRepository.suggest(
                amountCents = result.amountCents,
                type = result.type,
                merchantRaw = merchant,
                sourcePackage = sbn.packageName
            )
        }
    }
}
