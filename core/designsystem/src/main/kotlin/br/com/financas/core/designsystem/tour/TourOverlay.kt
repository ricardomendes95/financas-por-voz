package br.com.financas.core.designsystem.tour

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.financas.core.designsystem.R

/**
 * Máscara escura full-screen com um "buraco" recortado sobre [targetRect] (técnica de
 * spotlight: `BlendMode.Clear` numa camada offscreen), mais um balão de texto explicando
 * o passo atual. Não sabe nada sobre telas ou navegação — só desenha o que recebe.
 */
@Composable
fun TourOverlay(
    targetRect: Rect?,
    message: String,
    isLastStep: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (targetRect == null) return
    val density = LocalDensity.current
    val paddingPx = with(density) { 8.dp.toPx() }
    val cornerPx = with(density) { 12.dp.toPx() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // Necessário para o BlendMode.Clear abaixo "furar" só esta camada, em vez
                // de apagar o que está atrás na janela inteira.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(color = Color.Black.copy(alpha = 0.75f))
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(targetRect.left - paddingPx, targetRect.top - paddingPx),
                size = Size(targetRect.width + paddingPx * 2, targetRect.height + paddingPx * 2),
                cornerRadius = CornerRadius(cornerPx),
                blendMode = BlendMode.Clear
            )
        }

        val screenHeightPx = with(density) { maxHeight.toPx() }
        val showCardBelow = targetRect.center.y < screenHeightPx / 2

        Card(
            modifier = Modifier
                .align(if (showCardBelow) Alignment.BottomCenter else Alignment.TopCenter)
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message, style = MaterialTheme.typography.bodyLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.tour_skip)) }
                    TextButton(onClick = onNext) {
                        Text(stringResource(if (isLastStep) R.string.tour_finish else R.string.tour_next))
                    }
                }
            }
        }
    }
}
