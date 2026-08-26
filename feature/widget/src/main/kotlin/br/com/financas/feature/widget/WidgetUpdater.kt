package br.com.financas.feature.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import br.com.financas.core.common.YearMonthUtils
import br.com.financas.core.data.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Atualiza o widget por evento de escrita (Flow do Room), nunca por
 * polling (regra §11.11) — `provideGlance` só lê o estado que este
 * observador publica.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val clock: Clock
) {
    fun start(scope: CoroutineScope) {
        val yearMonth = YearMonthUtils.currentYearMonth(clock.zone)
        transactionRepository.observeMonthlySummary(yearMonth)
            .distinctUntilChanged()
            .onEach { summary ->
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(FinanceGlanceWidget::class.java)
                glanceIds.forEach { id ->
                    updateAppWidgetState(context, id) { prefs ->
                        prefs[FinanceWidgetState.BALANCE_CENTS] = summary.balanceCents
                        prefs[FinanceWidgetState.INCOME_CENTS] = summary.totalIncomeCents
                        prefs[FinanceWidgetState.EXPENSE_CENTS] = summary.totalExpenseCents
                    }
                    FinanceGlanceWidget().update(context, id)
                }
            }
            .launchIn(scope)
    }
}
