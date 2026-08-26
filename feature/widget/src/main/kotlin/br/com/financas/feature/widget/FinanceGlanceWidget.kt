package br.com.financas.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalState
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import br.com.financas.core.common.DeepLinks
import br.com.financas.core.common.MoneyFormatter
import br.com.financas.feature.voice.capture.VoiceCaptureActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.width
import androidx.compose.ui.unit.dp

/**
 * Rota 4 (§5.5): saldo do mês + microfone que abre a captura direto no modo
 * escuta + atalhos manuais de despesa/receita. `provideGlance` só lê o
 * estado já publicado em `Preferences` — nunca faz query síncrona aqui
 * (regra §11); quem escreve o estado é o `WidgetUpdater`, reagindo a Flow.
 */
class FinanceGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = LocalState.current as? Preferences
            val balance = prefs?.get(FinanceWidgetState.BALANCE_CENTS) ?: 0L
            WidgetContent(balance)
        }
    }
}

@Composable
private fun WidgetContent(balanceCents: Long) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF1A1D23)))
            .cornerRadius(16.dp)
            .padding(16.dp)
    ) {
        Text(
            text = "Saldo do mês",
            style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF94A3B8)))
        )
        Text(
            text = MoneyFormatter.format(balanceCents),
            style = TextStyle(
                color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                fontWeight = FontWeight.Bold
            )
        )
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WidgetIconButton(
                R.drawable.ic_widget_minus,
                contentDescription = "Adicionar despesa",
                deepLink = DeepLinks.add(type = DeepLinks.TYPE_EXPENSE)
            )
            androidx.glance.layout.Spacer(GlanceModifier.width(16.dp))
            WidgetMicButton()
            androidx.glance.layout.Spacer(GlanceModifier.width(16.dp))
            WidgetIconButton(
                R.drawable.ic_widget_plus,
                contentDescription = "Adicionar receita",
                deepLink = DeepLinks.add(type = DeepLinks.TYPE_INCOME)
            )
        }
    }
}

@Composable
private fun WidgetIconButton(iconRes: Int, contentDescription: String, deepLink: String) {
    Image(
        provider = ImageProvider(iconRes),
        contentDescription = contentDescription,
        modifier = GlanceModifier
            .size(36.dp)
            .clickable(
                actionRunCallback<OpenDeepLinkAction>(
                    actionParametersOf(OpenDeepLinkAction.KEY_DEEP_LINK to deepLink)
                )
            )
    )
}

@Composable
private fun WidgetMicButton() {
    Image(
        provider = ImageProvider(R.drawable.ic_widget_mic),
        contentDescription = "Lançar por voz",
        modifier = GlanceModifier
            .size(44.dp)
            .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF16A34A)))
            .cornerRadius(22.dp)
            .padding(10.dp)
            .clickable(actionStartActivity(VoiceCaptureActivity::class.java))
    )
}
