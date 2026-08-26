package br.com.financas.nlu

import br.com.financas.nlu.category.CategoryClassifier
import br.com.financas.nlu.extract.AmountExtractor
import br.com.financas.nlu.extract.DateExtractor
import br.com.financas.nlu.extract.DescriptionExtractor
import br.com.financas.nlu.extract.PaymentMethodExtractor
import br.com.financas.nlu.extract.TypeExtractor
import br.com.financas.nlu.model.AmbiguityField
import br.com.financas.nlu.model.DatePrecision
import br.com.financas.nlu.model.FailureReason
import br.com.financas.nlu.model.ParseContext
import br.com.financas.nlu.model.ParseResult
import br.com.financas.nlu.model.TransactionDraft
import br.com.financas.nlu.text.ConsumptionMask
import br.com.financas.nlu.text.TextNormalizer
import br.com.financas.nlu.text.Tokenizer

/**
 * Converte uma frase em português brasileiro num lançamento financeiro.
 *
 * Ponto único de interpretação do app: todas as oito rotas de entrada
 * (AppFunctions, App Actions, assistente do sistema, widget, tile, notificação,
 * deep link e automação externa) entregam texto cru aqui.
 *
 * O módulo é Kotlin puro e não depende de Android — roda em teste de JVM.
 */
interface TransactionParser {
    fun parse(rawText: String, context: ParseContext): ParseResult
}

class FinanceTextParser : TransactionParser {

    override fun parse(rawText: String, context: ParseContext): ParseResult {
        val trace = mutableListOf<String>()

        if (rawText.isBlank()) {
            return ParseResult.failure(FailureReason.EMPTY_INPUT, listOf("entrada vazia"))
        }

        val normalized = TextNormalizer.normalize(rawText)
        val tokens = Tokenizer.tokenize(normalized)
        val mask = ConsumptionMask(normalized.length)
        trace += "normalizado: '$normalized'"

        // 1. Data primeiro: garante que "dia 24" não vire valor.
        val date = DateExtractor.extract(normalized, context.now, mask)
        trace += "data → ${date.trace}"

        // 2. Forma de pagamento antes do valor: consome "no crédito", "no débito".
        val payment = PaymentMethodExtractor.extract(normalized, mask)
        payment?.let { trace += "pagamento → ${it.trace}" }

        // 3. Tipo: consome o verbo, que não deve sobrar na descrição.
        val type = TypeExtractor.extract(normalized, mask)
        trace += "tipo → ${type.trace}"

        // 4. Valor.
        val amount = AmountExtractor.extract(normalized, tokens, mask)
        if (amount == null) {
            trace += "valor → não encontrado"
            return ParseResult.failure(FailureReason.NO_AMOUNT_FOUND, trace)
        }
        trace += "valor → ${amount.trace}"

        // 5. Descrição: o que sobrou.
        val description = DescriptionExtractor.extract(rawText, normalized, mask)
        trace += "descrição → '$description'"

        // 6. Categoria, a partir da descrição.
        val category = CategoryClassifier.classify(description, type.type, context)
        trace += "categoria → ${category.trace}"

        val confidence = aggregateConfidence(
            amount = amount.confidence,
            category = category.confidence,
            date = dateConfidence(date.precision),
            type = type.confidence
        )

        val ambiguities = buildSet {
            if (amount.confidence < 0.80f) add(AmbiguityField.AMOUNT)
            if (category.confidence < 0.75f) add(AmbiguityField.CATEGORY)
            if (type.confidence < 0.80f) add(AmbiguityField.TYPE)
            if (description.isBlank()) add(AmbiguityField.DESCRIPTION)
        }

        val draft = TransactionDraft(
            amountCents = amount.cents,
            type = type.type,
            description = description.ifBlank { fallbackDescription(category.categoryId, context) },
            categoryId = category.categoryId,
            occurredAt = date.dateTime,
            datePrecision = date.precision,
            paymentMethod = payment?.method,
            rawInput = rawText.trim()
        )

        trace += "confiança agregada → %.2f".format(confidence)

        return ParseResult(
            draft = draft,
            confidence = confidence,
            ambiguities = ambiguities,
            failureReason = null,
            trace = trace
        )
    }

    /**
     * Pesos escolhidos pelo custo de errar cada campo: um valor errado
     * corrompe todo relatório, uma data errada só desloca um mês, e uma
     * categoria errada é a correção mais fácil de fazer depois.
     */
    private fun aggregateConfidence(
        amount: Float,
        category: Float,
        date: Float,
        type: Float
    ): Float = (amount * 0.40f + category * 0.30f + date * 0.15f + type * 0.15f)
        .coerceIn(0f, 1f)

    private fun dateConfidence(precision: DatePrecision): Float = when (precision) {
        DatePrecision.EXPLICIT_FULL -> 1.0f
        DatePrecision.EXPLICIT_DAY -> 0.90f
        DatePrecision.RELATIVE -> 0.95f
        DatePrecision.ASSUMED_NOW -> 0.85f
    }

    private fun fallbackDescription(categoryId: String, context: ParseContext): String =
        context.categories.firstOrNull { it.id == categoryId }?.name ?: "Lançamento"
}
