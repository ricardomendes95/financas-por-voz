package br.com.financas.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme(
    background = DarkSurfaceBase,
    surface = DarkSurfaceBase,
    surfaceVariant = DarkSurfaceElevated
)

/**
 * Material You / dynamic color por padrão (Android 12+), com fallback para
 * paleta própria (§10.3). Cores semânticas (income/expense/warning) ficam
 * disponíveis via `FinanceTheme.colors` dentro de qualquer composable.
 */
@Composable
fun FinancasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    val financeColors = if (darkTheme) {
        FinanceColors(income = IncomeDark, expense = ExpenseDark, warning = WarningDark)
    } else {
        FinanceColors(income = IncomeLight, expense = ExpenseLight, warning = WarningLight)
    }

    CompositionLocalProvider(LocalFinanceColors provides financeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FinanceTypography,
            content = content
        )
    }
}

/** Acesso ergonômico às cores semânticas: `FinanceTheme.colors.income`. */
object FinanceTheme {
    val colors: FinanceColors
        @Composable get() = LocalFinanceColors.current
}
