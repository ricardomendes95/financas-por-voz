package br.com.financas.feature.voice.capture

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import br.com.financas.core.designsystem.theme.FinancasTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity transparente sem chrome (§5.10) — usada pelas rotas 3 (assist),
 * 4 (widget), 5 (QS tile) e pelo deep link `financas://capture`.
 */
@AndroidEntryPoint
class VoiceCaptureActivity : ComponentActivity() {

    private val viewModel: VoiceCaptureViewModel by viewModels()

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startListening() else viewModel.onPermissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FinancasTheme {
                VoiceCaptureScreen(onClose = { finish() }, modifier = Modifier.fillMaxSize())
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startListening()
        } else {
            requestPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
