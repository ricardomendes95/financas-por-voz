package br.com.financas.feature.voice.capture

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.financas.core.model.EntrySource
import br.com.financas.feature.voice.notification.EntryNotifier
import br.com.financas.feature.voice.gateway.QuickEntryGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI da captura (§5.10): timeout de silêncio 1,8s, reconhecimento
 * preferencialmente on-device, resultado parcial em tempo real. Ao
 * terminar, chama `QuickEntryGateway.ingest` direto — nunca abre tela de
 * confirmação (regra §12).
 */
@HiltViewModel
class VoiceCaptureViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gateway: QuickEntryGateway,
    private val entryNotifier: EntryNotifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceCaptureUiState())
    val uiState: StateFlow<VoiceCaptureUiState> = _uiState.asStateFlow()

    private val events = Channel<VoiceCaptureEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private var recognizer: SpeechRecognizer? = null

    fun onPermissionDenied() {
        _uiState.update { it.copy(phase = CapturePhase.NO_PERMISSION) }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _uiState.update {
                it.copy(phase = CapturePhase.ERROR, errorMessage = "Reconhecimento de voz indisponível neste aparelho")
            }
            return
        }

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        speechRecognizer.setRecognitionListener(listener)

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // EXTRA_PREFER_OFFLINE removido: o motor offline do S23 falha com
            // ERROR_NO_MATCH (código 7) para frases em pt-BR nesse aparelho.
            // Sem essa flag o sistema usa reconhecimento online quando há
            // rede — muito mais preciso, ao custo de mandar o áudio da fala
            // para os servidores do provedor do reconhecedor padrão.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_800)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1_800)
        }
        _uiState.update { it.copy(phase = CapturePhase.LISTENING, partialText = "") }
        speechRecognizer.startListening(recognizerIntent)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _uiState.update { it.copy(amplitude = normalized) }
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            _uiState.update { it.copy(phase = CapturePhase.PROCESSING) }
        }

        override fun onError(error: Int) {
            finishWithTextOrError(null)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
            finishWithTextOrError(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (text != null) _uiState.update { it.copy(partialText = text) }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    /**
     * Trata tanto `onResults` (texto final vazio/nulo) quanto `onError`
     * (nenhum texto disponível): alguns reconhecedores mandam resultados
     * parciais corretos e depois falham em confirmar o resultado final —
     * o último texto parcial já visto é mais confiável que descartar tudo
     * e mostrar erro pro usuário que acabou de ver a frase certa na tela.
     */
    private fun finishWithTextOrError(finalText: String?) {
        val text = finalText?.takeIf { it.isNotBlank() } ?: _uiState.value.partialText.trim()
        if (text.isNotBlank()) {
            handleFinalText(text)
            return
        }
        _uiState.update {
            it.copy(phase = CapturePhase.ERROR, errorMessage = "Não entendi, tente de novo")
        }
        viewModelScope.launch {
            delay(1_200)
            events.send(VoiceCaptureEvent.Close)
        }
    }

    private fun handleFinalText(text: String) {
        _uiState.update { it.copy(phase = CapturePhase.PROCESSING, partialText = text) }
        viewModelScope.launch {
            val outcome = gateway.ingest(text, EntrySource.VOICE)
            entryNotifier.notifyOutcome(outcome)
            _uiState.update { it.copy(phase = CapturePhase.DONE) }
            delay(400)
            events.send(VoiceCaptureEvent.Close)
        }
    }

    override fun onCleared() {
        recognizer?.destroy()
        recognizer = null
    }
}
