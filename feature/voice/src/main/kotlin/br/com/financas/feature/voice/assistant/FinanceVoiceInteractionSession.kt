package br.com.financas.feature.voice.assistant

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import br.com.financas.feature.voice.capture.VoiceCaptureActivity

/**
 * Rota 3 (§5.4) — só existe se o usuário definir explicitamente o app como
 * assistente do sistema em Configurações → Voz (nunca sugerido de novo se
 * recusado). Sem UI própria: abre a captura de voz direto e some.
 */
class FinanceVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    override fun onHandleAssist(data: Bundle?, structure: AssistStructure?, content: AssistContent?) {
        val intent = Intent(context, VoiceCaptureActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startAssistantActivity(intent)
        finish()
    }
}
