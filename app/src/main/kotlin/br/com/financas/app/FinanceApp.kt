package br.com.financas.app

import android.app.Application
import br.com.financas.core.data.AppInitializer
import br.com.financas.feature.voice.notification.PersistentNotificationUpdater
import br.com.financas.feature.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sem I/O aqui (regra §6) — tudo abaixo é disparado numa coroutine em
 * `Dispatchers.IO` e não bloqueia `onCreate`; o Hilt também só constrói o
 * `AppDatabase` de forma lazy, na primeira injeção.
 */
@HiltAndroidApp
class FinanceApp : Application() {

    @Inject lateinit var appInitializer: AppInitializer
    @Inject lateinit var persistentNotificationUpdater: PersistentNotificationUpdater
    @Inject lateinit var widgetUpdater: WidgetUpdater

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            appInitializer.run()
            persistentNotificationUpdater.start(applicationScope)
            widgetUpdater.start(applicationScope)
        }
    }
}
