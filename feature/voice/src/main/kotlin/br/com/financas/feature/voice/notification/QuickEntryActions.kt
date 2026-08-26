package br.com.financas.feature.voice.notification

/** Ações e extras compartilhados entre `EntryNotifier` e `QuickEntryBroadcastReceiver`. */
object QuickEntryActions {
    const val ACTION_UNDO = "br.com.financas.action.UNDO_TRANSACTION"
    const val ACTION_REMOTE_INPUT = "br.com.financas.action.QUICK_ENTRY_REMOTE_INPUT"
    const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val REMOTE_INPUT_KEY = "quick_entry_text"

    const val CONFIRMATION_NOTIFICATION_ID = 1001
    const val PERSISTENT_NOTIFICATION_ID = 1002
}
