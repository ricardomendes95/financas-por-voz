package br.com.financas.core.data.tour

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tourDataStore by preferencesDataStore(name = "tour")

/** Guarda se o tour guiado já foi concluído (ou pulado) neste dispositivo. */
@Singleton
class TourPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val completedKey = booleanPreferencesKey("completed")

    fun observeCompleted(): Flow<Boolean> = context.tourDataStore.data.map { it[completedKey] ?: false }

    suspend fun markCompleted() {
        context.tourDataStore.edit { it[completedKey] = true }
    }
}
