package br.com.financas.core.model

/** Conta ou carteira onde o lançamento acontece — "Nubank", "Carteira", "Inter". */
data class Account(
    val id: String,
    val name: String,
    val kind: AccountKind,
    val openingBalanceCents: Long,
    val closingDay: Int? = null,
    val dueDay: Int? = null,
    val colorArgb: Int
) {
    companion object {
        const val DEFAULT_ID = "default"
    }
}
