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
import br.com.financas.app.navigation.FinanceNavHost
import br.com.financas.core.common.DeepLinks
import br.com.financas.core.designsystem.theme.FinancasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingDeepLink by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeepLink = intent?.data

        // Cold start sem deep link explícito ("Ok Google, abrir Finanças" e
        // tocar no ícone disparam o mesmo Intent MAIN/LAUNCHER, sem como
        // diferenciar um do outro) já abre a captura de voz por cima, sem
        // passar pelo Dashboard — savedInstanceState == null garante que só
        // dispara na criação real da Activity, não numa recriação por
        // rotação/config change.
        if (savedInstanceState == null && intent?.data == null) {
            val captureIntent = Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinks.capture()))
                .setPackage(packageName)
            startActivity(captureIntent)
        }

        setContent {
            FinancasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FinanceNavHost(deepLinkUri = pendingDeepLink)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = intent.data
    }
}
