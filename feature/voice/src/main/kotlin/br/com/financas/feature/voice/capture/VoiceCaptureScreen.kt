package br.com.financas.feature.voice.capture

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun VoiceCaptureScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VoiceCaptureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                VoiceCaptureEvent.Close -> onClose()
            }
        }
    }

    // Haptics (§10.3): LongPress ao começar a gravar, Confirm ao terminar de entender a fala.
    LaunchedEffect(uiState.phase) {
        when (uiState.phase) {
            CapturePhase.LISTENING -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            CapturePhase.DONE -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            else -> Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PulsingMic(uiState)
            Spacer(Modifier.height(12.dp))
            Text(
                text = statusText(uiState),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun PulsingMic(uiState: VoiceCaptureUiState) {
    val scale by animateFloatAsState(
        targetValue = 1f + (uiState.amplitude * 0.4f),
        animationSpec = spring(dampingRatio = 0.8f),
        label = "mic-scale"
    )
    val circleColor = when (uiState.phase) {
        CapturePhase.ERROR, CapturePhase.NO_PERMISSION -> MaterialTheme.colorScheme.error
        CapturePhase.DONE -> Color(0xFF16A34A)
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(if (uiState.phase == CapturePhase.LISTENING) scale else 1f)
            .background(circleColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (uiState.phase == CapturePhase.DONE) Icons.Filled.Check else Icons.Filled.Mic,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
    }
}

private fun statusText(uiState: VoiceCaptureUiState): String = when (uiState.phase) {
    CapturePhase.LISTENING -> uiState.partialText.ifBlank { "Fale seu gasto..." }
    CapturePhase.PROCESSING -> uiState.partialText.ifBlank { "Processando..." }
    CapturePhase.DONE -> "Registrado!"
    CapturePhase.ERROR -> uiState.errorMessage ?: "Não entendi"
    CapturePhase.NO_PERMISSION -> "Permissão de microfone necessária"
}
