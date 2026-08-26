package br.com.financas.feature.voice.notification

import br.com.financas.core.common.YearMonthUtils
import br.com.financas.core.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mantém a notificação persistente da rota 6 (§5.7) sempre com o saldo do
 * mês e o gasto de hoje em dia — atualizada por evento de escrita via Flow,
 * nunca por polling (regra §11).
 */
@Singleton
class PersistentNotificationUpdater @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val entryNotifier: EntryNotifier,
    private val clock: Clock
) {
    fun start(scope: CoroutineScope) {
        val yearMonth = YearMonthUtils.currentYearMonth(clock.zone)
        combine(
            transactionRepository.observeMonthlySummary(yearMonth),
            transactionRepository.observeTodaySpent()
        ) { summary, todaySpent -> summary.balanceCents to todaySpent }
            .distinctUntilChanged()
            .onEach { (balance, todaySpent) -> entryNotifier.updatePersistent(balance, todaySpent) }
            .launchIn(scope)
    }
}
