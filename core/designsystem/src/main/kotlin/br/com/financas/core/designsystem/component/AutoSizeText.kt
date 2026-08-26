package br.com.financas.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * Texto de uma linha só cuja fonte encolhe até caber na largura disponível —
 * usada no saldo do Dashboard: "R$ 100,00" usa a fonte máxima (destaque),
 * "R$ 14.169,15" reduz até parar de quebrar linha, em vez de estourar o card.
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxFontSize: TextUnit = style.fontSize,
    minFontSize: TextUnit = 24.sp
) {
    var fontSize by remember(text, maxFontSize) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text, maxFontSize) { mutableStateOf(false) }

    Box(modifier = modifier) {
        Text(
            text = text,
            style = style,
            color = color,
            fontSize = fontSize,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.drawWithContent { if (readyToDraw) drawContent() },
            onTextLayout = { result ->
                val current = fontSize
                if (result.didOverflowWidth && current.isSpecified && current > minFontSize) {
                    fontSize = (current.value * 0.92f).coerceAtLeast(minFontSize.value).sp
                } else {
                    readyToDraw = true
                }
            }
        )
    }
}
