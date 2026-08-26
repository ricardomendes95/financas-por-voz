package br.com.financas.feature.voice.notification

import br.com.financas.feature.voice.gateway.IngestOutcome

/**
 * Feedback obrigatório em todas as rotas (§5.11): notificação heads-up de
 * confirmação com Desfazer/Editar, e a notificação persistente da rota 6.
 */
interface EntryNotifier {
    fun notifyOutcome(outcome: IngestOutcome)
    fun cancelConfirmation()
    fun updatePersistent(balanceCents: Long, todaySpentCents: Long)
    fun hidePersistent()
}
