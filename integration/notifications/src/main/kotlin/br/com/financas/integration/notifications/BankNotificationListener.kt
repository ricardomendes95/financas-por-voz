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
            // Alguns formatos (ex.: "Compra no débito aprovada" do Nubank) só têm a palavra
            // "aprovada" no título — juntar os dois dá mais chance de bater numa regra.
            val title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val combined = if (title.isBlank()) text else "$title\n$text"
            val result = BankMessageParser.parse(combined) ?: return@launch
            val merchant = MerchantNormalizer.normalize(result.merchantRaw)

            suggestionRepository.suggest(
                amountCents = result.amountCents,
                type = result.type,
                merchantRaw = merchant,
                sourcePackage = sbn.packageName,
                // Hora real da compra/transferência — não a hora em que o app processou a
                // notificação, que pode chegar alguns segundos depois via WorkManager/scope.
                detectedAt = sbn.postTime
            )
        }
    }
}
