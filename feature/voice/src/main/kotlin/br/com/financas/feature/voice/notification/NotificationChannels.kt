package br.com.financas.feature.voice.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val QUICK_ENTRY = "quick_entry"
    const val CONFIRMATION = "confirmation"

    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(QUICK_ENTRY, "Lançamento rápido", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Saldo do mês e atalho para lançar gastos por texto ou voz"
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CONFIRMATION, "Confirmação de lançamento", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Aviso rápido depois de registrar um gasto ou receita por voz"
            }
        )
    }
}
