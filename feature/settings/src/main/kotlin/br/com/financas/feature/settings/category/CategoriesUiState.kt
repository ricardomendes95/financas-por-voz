package br.com.financas.feature.settings.category

import br.com.financas.core.model.Category

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
)

/** Paleta usada tanto pelas categorias de fábrica (`CategorySeeder`) quanto pelas criadas pelo usuário. */
object CategoryPalette {
    val COLORS: List<Int> = listOf(
        0xFFF97316.toInt(), // laranja
        0xFF3B82F6.toInt(), // azul
        0xFF8B5CF6.toInt(), // roxo
        0xFFEF4444.toInt(), // vermelho
        0xFFEC4899.toInt(), // rosa
        0xFF14B8A6.toInt(), // teal
        0xFFF59E0B.toInt(), // âmbar
        0xFF64748B.toInt(), // cinza azulado
        0xFF84CC16.toInt(), // lima
        0xFF78716C.toInt(), // marrom
        0xFF22C55E.toInt(), // verde
        0xFF10B981.toInt(), // esmeralda
        0xFF06B6D4.toInt(), // ciano
        0xFF22D3EE.toInt()  // ciano claro
    )

    val ICONS: List<String> = listOf(
        "restaurant", "directions_car", "home", "favorite", "movie", "school",
        "shopping_bag", "build", "pets", "receipt_long", "payments", "work",
        "undo", "add_circle", "more_horiz"
    )
}
