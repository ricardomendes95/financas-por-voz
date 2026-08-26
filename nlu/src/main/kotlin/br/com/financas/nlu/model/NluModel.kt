package br.com.financas.nlu.model

import java.time.LocalDateTime

/** Saída ou entrada de dinheiro. */
enum class TransactionType { EXPENSE, INCOME }

/** Forma de pagamento, quando mencionada explicitamente. */
enum class PaymentMethod { PIX, CREDIT, DEBIT, CASH, BOLETO, TRANSFER }

/** Precisão com que a data foi determinada — usada para pedir revisão. */
enum class DatePrecision {
    /** Nenhuma data no texto: assumiu o momento atual. */
    ASSUMED_NOW,

    /** Expressão relativa resolvida ("ontem", "sexta passada"). */
    RELATIVE,

    /** Dia explícito, mês inferido ("dia 24"). */
    EXPLICIT_DAY,

    /** Data completa no texto ("24 de julho", "24/07"). */
    EXPLICIT_FULL
}

/** Campo que o parser não conseguiu determinar com segurança. */
enum class AmbiguityField { AMOUNT, TYPE, CATEGORY, DATE, DESCRIPTION }

/**
 * Categoria disponível para classificação. O módulo :nlu não conhece Room —
 * quem chama injeta a lista vinda do banco.
 */
data class CategoryRef(
    val id: String,
    val name: String,
    val type: TransactionType?
)

/**
 * Regra palavra-chave → categoria.
 *
 * @param keyword deve estar normalizada (minúscula, sem acento).
 * @param weight desempate quando várias regras casam. Regras do usuário nascem
 *   com peso maior que as de fábrica.
 */
data class CategoryRule(
    val keyword: String,
    val categoryId: String,
    val weight: Int = 10,
    val userDefined: Boolean = false
)

/** Contexto de uma chamada de parse. */
data class ParseContext(
    val now: LocalDateTime,
    val categories: List<CategoryRef>,
    val rules: List<CategoryRule>,
    val fallbackExpenseCategoryId: String,
    val fallbackIncomeCategoryId: String
)

/** Lançamento extraído do texto, ainda não persistido. */
data class TransactionDraft(
    val amountCents: Long,
    val type: TransactionType,
    val description: String,
    val categoryId: String,
    val occurredAt: LocalDateTime,
    val datePrecision: DatePrecision,
    val paymentMethod: PaymentMethod?,
    val rawInput: String
)

/** Resultado completo, incluindo diagnóstico. */
data class ParseResult(
    val draft: TransactionDraft?,
    val confidence: Float,
    val ambiguities: Set<AmbiguityField>,
    val failureReason: FailureReason?,
    val trace: List<String>
) {
    val isSuccess: Boolean get() = draft != null

    /** true quando o app deve marcar `needsReview` no lançamento. */
    val needsReview: Boolean get() = confidence < CONFIDENCE_AUTO_ACCEPT

    /** true quando o app deve abrir o bottom sheet de correção. */
    val needsCorrection: Boolean get() = confidence < CONFIDENCE_NEEDS_SHEET

    companion object {
        const val CONFIDENCE_AUTO_ACCEPT = 0.85f
        const val CONFIDENCE_NEEDS_SHEET = 0.60f

        fun failure(reason: FailureReason, trace: List<String> = emptyList()) = ParseResult(
            draft = null,
            confidence = 0f,
            ambiguities = emptySet(),
            failureReason = reason,
            trace = trace
        )
    }
}

enum class FailureReason {
    EMPTY_INPUT,
    NO_AMOUNT_FOUND,
    AMOUNT_OUT_OF_RANGE
}
