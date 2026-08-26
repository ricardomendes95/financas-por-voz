package br.com.financas.feature.voice.capture

enum class CapturePhase { LISTENING, PROCESSING, DONE, ERROR, NO_PERMISSION }

data class VoiceCaptureUiState(
    val phase: CapturePhase = CapturePhase.LISTENING,
    val amplitude: Float = 0f,
    val partialText: String = "",
    val errorMessage: String? = null
)

sealed interface VoiceCaptureEvent {
    data object Close : VoiceCaptureEvent
}
