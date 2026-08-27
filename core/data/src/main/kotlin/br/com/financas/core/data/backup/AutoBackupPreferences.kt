package br.com.financas.core.data.backup

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.autoBackupDataStore by preferencesDataStore(name = "auto_backup")

/**
 * Liga/desliga o Android Auto Backup (o backup manual via SAF continua
 * disponível de qualquer forma). Ativo por padrão — o usuário desativa em
 * Configurações se não quiser.
 */
@Singleton
class AutoBackupPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val enabledKey = booleanPreferencesKey("enabled")

    fun observeEnabled(): Flow<Boolean> = context.autoBackupDataStore.data.map { it[enabledKey] ?: true }

    suspend fun setEnabled(enabled: Boolean) {
        context.autoBackupDataStore.edit { it[enabledKey] = enabled }
    }

    companion object {
        /**
         * Leitura síncrona usada pelo `AutoBackupAgent` — ele roda fora do ciclo de vida
         * normal do app (sem `@AndroidEntryPoint`), então não dá pra injetar esta classe
         * via Hilt nem usar coroutines estruturadas; o sistema espera uma resposta rápida.
         */
        fun isEnabledBlocking(context: Context): Boolean =
            runBlocking { AutoBackupPreferences(context).observeEnabled().first() }
    }
}
