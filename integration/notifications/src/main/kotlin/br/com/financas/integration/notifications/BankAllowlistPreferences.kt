package br.com.financas.integration.notifications

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.bankAllowlistDataStore by preferencesDataStore(name = "bank_allowlist")

/** Feature 100% opt-in (§8.4.5) — nenhum pacote habilitado por padrão. */
@Singleton
class BankAllowlistPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val enabledKey = stringSetPreferencesKey("enabled_packages")

    fun observeEnabledPackages(): Flow<Set<String>> =
        context.bankAllowlistDataStore.data.map { it[enabledKey] ?: emptySet() }

    suspend fun setEnabled(packageName: String, enabled: Boolean) {
        context.bankAllowlistDataStore.edit { prefs ->
            val current = prefs[enabledKey] ?: emptySet()
            prefs[enabledKey] = if (enabled) current + packageName else current - packageName
        }
    }
}
