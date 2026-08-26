package br.com.financas.feature.voice.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import br.com.financas.core.common.DeepLinks
import br.com.financas.core.common.MoneyFormatter
import br.com.financas.feature.voice.R
import br.com.financas.feature.voice.gateway.IngestOutcome
import br.com.financas.feature.voice.notification.QuickEntryActions.ACTION_REMOTE_INPUT
import br.com.financas.feature.voice.notification.QuickEntryActions.ACTION_UNDO
import br.com.financas.feature.voice.notification.QuickEntryActions.CONFIRMATION_NOTIFICATION_ID
import br.com.financas.feature.voice.notification.QuickEntryActions.EXTRA_TRANSACTION_ID
import br.com.financas.feature.voice.notification.QuickEntryActions.PERSISTENT_NOTIFICATION_ID
import br.com.financas.feature.voice.notification.QuickEntryActions.REMOTE_INPUT_KEY
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidEntryNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : EntryNotifier {

    private val manager = NotificationManagerCompat.from(context)

    init {
        NotificationChannels.ensureCreated(context)
    }

    override fun notifyOutcome(outcome: IngestOutcome) {
        if (!manager.areNotificationsEnabled()) return
        when (outcome) {
            is IngestOutcome.Recorded -> notifyRecorded(outcome)
            is IngestOutcome.NotUnderstood -> notifyNotUnderstood(outcome.rawText)
        }
    }

    private fun notifyRecorded(outcome: IngestOutcome.Recorded) {
        val title = if (outcome.isExpense) "Despesa registrada" else "Receita registrada"
        val amount = MoneyFormatter.formatSigned(outcome.amountCents, outcome.isExpense)
        val body = "$amount · ${outcome.description}\n${outcome.categoryName}"

        val undoIntent = broadcastPendingIntent(
            requestCode = outcome.transactionId.hashCode(),
            action = ACTION_UNDO,
            extras = mapOf(EXTRA_TRANSACTION_ID to outcome.transactionId)
        )
        val editIntent = activityPendingIntent(
            requestCode = outcome.transactionId.hashCode() + 1,
            uri = DeepLinks.edit(outcome.transactionId)
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CONFIRMATION)
            .setSmallIcon(R.drawable.ic_stat_finance)
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentText(body.replace('\n', ' '))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setTimeoutAfter(4_000)
            .addAction(0, "Desfazer", undoIntent)
            .addAction(0, "Editar", editIntent)
            .build()

        // Android 13+ exige a permissão em runtime — sem o guard literal
        // abaixo, `notify` pode lançar `SecurityException` (regra do lint).
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(CONFIRMATION_NOTIFICATION_ID, notification)
        }
    }

    private fun notifyNotUnderstood(rawText: String) {
        val notification = NotificationCompat.Builder(context, NotificationChannels.CONFIRMATION)
            .setSmallIcon(R.drawable.ic_stat_finance)
            .setContentTitle("Não entendi o valor")
            .setContentText("\"$rawText\" — toque para lançar manualmente")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setTimeoutAfter(4_000)
            .setContentIntent(activityPendingIntent(rawText.hashCode(), DeepLinks.add(rawText)))
            .build()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(CONFIRMATION_NOTIFICATION_ID, notification)
        }
    }

    override fun cancelConfirmation() {
        manager.cancel(CONFIRMATION_NOTIFICATION_ID)
    }

    override fun updatePersistent(balanceCents: Long, todaySpentCents: Long) {
        if (!manager.areNotificationsEnabled()) return

        val remoteInput = RemoteInput.Builder(REMOTE_INPUT_KEY)
            .setLabel("Ex.: 20 reais de pastel")
            .build()
        val replyIntent = broadcastPendingIntent(
            requestCode = PERSISTENT_NOTIFICATION_ID,
            action = ACTION_REMOTE_INPUT,
            extras = emptyMap(),
            mutable = true
        )
        val replyAction = NotificationCompat.Action.Builder(0, "Lançar", replyIntent)
            .addRemoteInput(remoteInput)
            .build()

        val notification = NotificationCompat.Builder(context, NotificationChannels.QUICK_ENTRY)
            .setSmallIcon(R.drawable.ic_stat_finance)
            .setContentTitle("Saldo do mês: ${MoneyFormatter.format(balanceCents)}")
            .setContentText("Hoje: ${MoneyFormatter.format(todaySpentCents)}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(activityPendingIntent(0, DeepLinks.add()))
            .addAction(replyAction)
            .build()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            manager.notify(PERSISTENT_NOTIFICATION_ID, notification)
        }
    }

    override fun hidePersistent() {
        manager.cancel(PERSISTENT_NOTIFICATION_ID)
    }

    private fun broadcastPendingIntent(
        requestCode: Int,
        action: String,
        extras: Map<String, String>,
        mutable: Boolean = false
    ): PendingIntent {
        val intent = Intent(action).setPackage(context.packageName)
        extras.forEach { (key, value) -> intent.putExtra(key, value) }
        val flags = (if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE) or
            PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    private fun activityPendingIntent(requestCode: Int, uri: String): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage(context.packageName)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
