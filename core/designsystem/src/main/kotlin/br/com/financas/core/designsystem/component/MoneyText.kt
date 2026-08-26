package br.com.financas.core.designsystem.component

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import br.com.financas.core.common.MoneyFormatter
import br.com.financas.core.designsystem.theme.MoneyTextStyle

/**
 * Valor monetário com tabular figures (§10.2) e sinal explícito — nunca só a
 * cor, para acessibilidade (§10.3).
 */
@Composable
fun MoneyText(
    cents: Long,
    isExpense: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current
) {
    Text(
        text = MoneyFormatter.formatSigned(cents, isExpense),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        style = style.merge(MoneyTextStyle)
    )
}
