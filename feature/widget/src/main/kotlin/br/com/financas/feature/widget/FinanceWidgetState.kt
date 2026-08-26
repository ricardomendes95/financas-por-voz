package br.com.financas.feature.widget

import androidx.datastore.preferences.core.longPreferencesKey

/** Chaves do `Preferences` do Glance — o `provideGlance` só lê isto, nunca faz query. */
object FinanceWidgetState {
    val BALANCE_CENTS = longPreferencesKey("balance_cents")
    val INCOME_CENTS = longPreferencesKey("income_cents")
    val EXPENSE_CENTS = longPreferencesKey("expense_cents")
}
