package br.com.financas.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import br.com.financas.app.backup.CorruptedBackupScreen
import br.com.financas.app.navigation.FinanceNavHost
import br.com.financas.core.common.DeepLinks
import br.com.financas.core.data.backup.DatabaseIntegrityChecker
import br.com.financas.core.designsystem.theme.FinancasTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface BootState {
    data object Checking : BootState
    data object Ready : BootState
    data object CorruptedBackup : BootState
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingDeepLink by mutableStateOf<Uri?>(null)
    private var bootState by mutableStateOf<BootState>(BootState.Checking)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeepLink = intent?.data
        val isColdStart = savedInstanceState == null

        setContent {
            FinancasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (bootState) {
                        BootState.Checking -> Unit
                        BootState.CorruptedBackup -> CorruptedBackupScreen(onContinueAnyway = ::discardAndContinue)
                        BootState.Ready -> FinanceNavHost(deepLinkUri = pendingDeepLink)
                    }
                }
            }
        }

        if (isColdStart) {
            checkBootState()
        } else {
            bootState = BootState.Ready
        }
    }

    /**
     * Só dispara a checagem quando o pacote instalado é genuinamente novo neste
     * dispositivo — `firstInstallTime == lastUpdateTime` é verdade apenas antes da
     * primeira atualização, ou seja, exatamente na primeira execução após instalar (não
     * numa atualização normal de uma versão para outra, que só muda `lastUpdateTime`).
     * Uma preferência local própria não serviria: ela não existiria em nenhuma instalação
     * anterior à introdução desta feature, fazendo toda atualização parecer uma restauração.
     */
    private fun isFreshInstall(): Boolean = runCatching {
        val info = packageManager.getPackageInfo(packageName, 0)
        info.firstInstallTime == info.lastUpdateTime
    }.getOrDefault(false)

    /**
     * Só na primeira execução de uma instalação nova: se o banco já existe, ele veio do
     * Android Auto Backup (ou de uma transferência de aparelho) — valida a integridade
     * antes de deixar o Room tocar nele (regra §3/§6: nunca abrir um banco corrompido
     * silenciosamente).
     */
    private fun checkBootState() {
        lifecycleScope.launch {
            val cameFromRestore = isFreshInstall()
            val valid = withContext(Dispatchers.IO) {
                !cameFromRestore || DatabaseIntegrityChecker.isValid(applicationContext)
            }
            if (!valid) {
                bootState = BootState.CorruptedBackup
                return@launch
            }
            bootState = BootState.Ready
            launchVoiceCaptureIfColdStart()
        }
    }

    private fun discardAndContinue() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { DatabaseIntegrityChecker.discard(applicationContext) }
            bootState = BootState.Ready
            launchVoiceCaptureIfColdStart()
        }
    }

    // "Ok Google, abrir Finanças" e tocar no ícone disparam o mesmo Intent MAIN/LAUNCHER,
    // sem como diferenciar um do outro — cold start sem deep link explícito já abre a
    // captura de voz por cima, sem passar pelo Dashboard.
    private fun launchVoiceCaptureIfColdStart() {
        if (intent?.data == null) {
            val captureIntent = Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinks.capture()))
                .setPackage(packageName)
            startActivity(captureIntent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = intent.data
    }
}
