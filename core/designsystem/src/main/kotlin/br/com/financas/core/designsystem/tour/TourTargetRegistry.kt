package br.com.financas.core.designsystem.tour

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Mapa compartilhado de id → posição na tela (coordenadas de janela) dos elementos que
 * podem ser destacados pelo tour guiado. Elementos se registram incondicionalmente com
 * [Modifier.tourTarget] — só o overlay decide, a partir do passo atual, o que desenhar.
 */
object TourTargetRegistry {
    private val positions = mutableStateMapOf<String, Rect>()

    fun register(id: String, rect: Rect) {
        positions[id] = rect
    }

    fun get(id: String): Rect? = positions[id]
}

/** Marca este elemento como um alvo destacável pelo tour guiado. */
fun Modifier.tourTarget(id: String): Modifier = onGloballyPositioned { coordinates ->
    if (coordinates.isAttached) {
        TourTargetRegistry.register(id, coordinates.boundsInWindow())
    }
}
