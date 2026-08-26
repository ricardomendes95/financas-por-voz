package br.com.financas.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Cores semânticas fora do `ColorScheme` padrão do Material3 — income/expense/warning. */
data class FinanceColors(
    val income: Color,
    val expense: Color,
    val warning: Color
)

val LocalFinanceColors = staticCompositionLocalOf {
    FinanceColors(income = IncomeLight, expense = ExpenseLight, warning = WarningLight)
}
