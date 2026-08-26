package br.com.financas.nlu.extract

import br.com.financas.nlu.text.ConsumptionMask
import br.com.financas.nlu.text.PortugueseNumbers
import br.com.financas.nlu.text.Token
import kotlin.math.roundToLong

/**
 * Extrai o valor monetário em **centavos**.
 *
 * Roda depois do extrator de datas, então trechos como "dia 24" já estão
 * marcados na máscara e não competem pelo papel de valor.
 */
object AmountExtractor {

    private const val MAX_CENTS = 99_999_999_99L // R$ 99.999.999,99

    private const val CURRENCY_WORDS = "reais|real|conto|contos|pila|pilas|mango|mangos|pau|paus|prata"

    data class Result(
        val cents: Long,
        val confidence: Float,
        val trace: String
    )

    fun extract(
        normalized: String,
        tokens: List<Token>,
        mask: ConsumptionMask
    ): Result? {
        // A ordem é uma escala de certeza decrescente.
        digitsWithCentavos(normalized, mask)?.let { return it }
        digitsWithMultiplier(normalized, mask)?.let { return it }
        digitsWithCurrencySymbol(normalized, mask)?.let { return it }
        digitsWithCurrencyWord(normalized, mask)?.let { return it }
        wordNumber(tokens, normalized, mask)?.let { return it }
        reaisAndCentsShorthand(normalized, mask)?.let { return it }
        decimalLookingBare(normalized, mask)?.let { return it }
        bareDigits(normalized, mask)?.let { return it }
        return null
    }

    // ---- "20 reais e 50 centavos" -------------------------------------------

    private val DIGITS_CENTAVOS = Regex(
        """\b(\d{1,3}(?:\.\d{3})*|\d+)\s*(?:$CURRENCY_WORDS)?\s*e\s*(\d{1,2})\s*centavos?\b"""
    )

    private fun digitsWithCentavos(text: String, mask: ConsumptionMask): Result? {
        val match = DIGITS_CENTAVOS.find(text) ?: return null
        if (mask.overlaps(match.range)) return null
        val reais = match.groupValues[1].replace(".", "").toLongOrNull() ?: return null
        val centavos = normalizeCentavos(match.groupValues[2])
        val cents = reais * 100 + centavos
        if (cents > MAX_CENTS) return null
        mask.consume(match.range)
        return Result(cents, 1.0f, "valor com centavos explícitos: $cents")
    }

    // ---- "1,5k", "2 mil", "1.5 mil" ------------------------------------------

    private val DIGITS_MULTIPLIER = Regex("""(?:r\s*\$\s*)?\b(\d+(?:[.,]\d+)?)\s*(k|mil)\b""")

    private fun digitsWithMultiplier(text: String, mask: ConsumptionMask): Result? {
        for (match in DIGITS_MULTIPLIER.findAll(text)) {
            if (mask.overlaps(match.range)) continue
            val base = parseDecimal(match.groupValues[1]) ?: continue
            val cents = base * 1000
            if (cents > MAX_CENTS) continue
            mask.consume(match.range)
            return Result(cents, 0.95f, "valor com multiplicador mil: $cents")
        }
        return null
    }

    // ---- "R$ 20,50" ----------------------------------------------------------

    private val DIGITS_SYMBOL = Regex("""r\s*\$\s*(\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?|\d+(?:[.,]\d{1,2})?)""")

    private fun digitsWithCurrencySymbol(text: String, mask: ConsumptionMask): Result? {
        for (match in DIGITS_SYMBOL.findAll(text)) {
            if (mask.overlaps(match.range)) continue
            val cents = parseBrazilianAmount(match.groupValues[1]) ?: continue
            if (cents > MAX_CENTS) continue
            mask.consume(match.range)
            return Result(cents, 1.0f, "valor com símbolo R$: $cents")
        }
        return null
    }

    // ---- "20 reais", "20 conto" ----------------------------------------------

    private val DIGITS_WORD = Regex(
        """\b(\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?|\d+(?:[.,]\d{1,2})?)\s*(?:$CURRENCY_WORDS)\b"""
    )

    private fun digitsWithCurrencyWord(text: String, mask: ConsumptionMask): Result? {
        for (match in DIGITS_WORD.findAll(text)) {
            if (mask.overlaps(match.range)) continue
            val cents = parseBrazilianAmount(match.groupValues[1]) ?: continue
            if (cents > MAX_CENTS) continue
            mask.consume(match.range)
            return Result(cents, 1.0f, "valor com palavra de moeda: $cents")
        }
        return null
    }

    // ---- "cento e vinte reais", "vinte reais e cinquenta centavos" -----------

    private fun wordNumber(
        tokens: List<Token>,
        text: String,
        mask: ConsumptionMask
    ): Result? {
        var i = 0
        while (i < tokens.size) {
            val run = PortugueseNumbers.consumeRun(tokens, i)
            if (run == null || mask.overlaps(run.range)) {
                i++
                continue
            }

            var cents = run.value * 100
            var endRange = run.range
            var confidence = 0.80f

            // A palavra de moeda logo depois eleva a certeza.
            val afterIndex = run.endTokenIndexExclusive
            val after = tokens.getOrNull(afterIndex)?.text
            if (after != null && Regex("^($CURRENCY_WORDS)$").matches(after)) {
                endRange = run.range.first until tokens[afterIndex].end
                confidence = 1.0f
            }

            // "... e cinquenta centavos"
            val tailStart = if (confidence == 1.0f) afterIndex + 1 else afterIndex
            if (tokens.getOrNull(tailStart)?.text == "e") {
                val centsRun = PortugueseNumbers.consumeRun(tokens, tailStart + 1)
                val centsWord = tokens.getOrNull(centsRun?.endTokenIndexExclusive ?: -1)?.text
                if (centsRun != null && centsWord != null && centsWord.startsWith("centavo")) {
                    cents += centsRun.value.coerceIn(0, 99)
                    endRange = run.range.first until tokens[centsRun.endTokenIndexExclusive].end
                    confidence = 1.0f
                }
            }

            if (cents > MAX_CENTS) {
                i = run.endTokenIndexExclusive
                continue
            }

            // "um"/"uma" isolado é ambíguo com artigo indefinido ("um dinheirão",
            // "uma grana") — só vira valor se vier acompanhado de moeda ou centavos.
            val isAmbiguousArticle = confidence == 0.80f &&
                run.firstTokenIndex == run.endTokenIndexExclusive - 1 &&
                tokens[run.firstTokenIndex].text in setOf("um", "uma")
            if (isAmbiguousArticle) {
                i = run.endTokenIndexExclusive
                continue
            }

            mask.consume(endRange)
            return Result(cents, confidence, "numeral por extenso: $cents")
        }
        return null
    }

    // ---- "23 e 50" (reais e centavos ditos sem a palavra) --------------------

    private val SHORTHAND = Regex("""\b(\d{1,4})\s+e\s+(\d{1,2})\b""")

    private fun reaisAndCentsShorthand(text: String, mask: ConsumptionMask): Result? {
        val match = SHORTHAND.find(text) ?: return null
        if (mask.overlaps(match.range)) return null
        val reais = match.groupValues[1].toLongOrNull() ?: return null
        val centavos = normalizeCentavos(match.groupValues[2])
        val cents = reais * 100 + centavos
        if (cents > MAX_CENTS) return null
        mask.consume(match.range)
        return Result(cents, 0.75f, "abreviação reais-e-centavos: $cents")
    }

    // ---- "45,90" sem palavra de moeda — o formato decimal já denuncia valor --

    private val DECIMAL_LOOKING = Regex("""\b\d{1,3}(?:\.\d{3})*,\d{2}\b""")

    /**
     * "45,90" sozinho é ambíguo em teoria, mas o formato — vírgula com
     * exatamente duas casas — é quase exclusivo de valor monetário em
     * pt-BR, então merece confiança bem acima de um número solto genérico
     * ("50 xyzabc").
     */
    private fun decimalLookingBare(text: String, mask: ConsumptionMask): Result? {
        for (match in DECIMAL_LOOKING.findAll(text)) {
            if (mask.overlaps(match.range)) continue
            val cents = parseBrazilianAmount(match.value) ?: continue
            if (cents > MAX_CENTS) continue
            mask.consume(match.range)
            return Result(cents, 0.85f, "valor decimal sem palavra de moeda: $cents")
        }
        return null
    }

    // ---- número solto --------------------------------------------------------

    private val BARE = Regex("""\b(\d{1,3}(?:\.\d{3})*(?:,\d{1,2})?|\d+(?:[.,]\d{1,2})?)\b""")

    private fun bareDigits(text: String, mask: ConsumptionMask): Result? {
        for (match in BARE.findAll(text)) {
            if (mask.overlaps(match.range)) continue
            val cents = parseBrazilianAmount(match.groupValues[1]) ?: continue
            if (cents > MAX_CENTS || cents == 0L) continue
            mask.consume(match.range)
            return Result(cents, 0.70f, "número solto interpretado como valor: $cents")
        }
        return null
    }

    // ---- utilidades ----------------------------------------------------------

    /** "50" → 50 centavos; "5" → 50 centavos (o falante disse "cinquenta"). */
    private fun normalizeCentavos(raw: String): Long {
        val value = raw.toLongOrNull() ?: return 0
        return if (raw.length == 1) value * 10 else value.coerceIn(0, 99)
    }

    /**
     * Interpreta número no formato brasileiro e devolve centavos.
     * "1.234,56" → 123456 · "45,90" → 4590 · "20" → 2000 · "1.500" → 150000
     */
    fun parseBrazilianAmount(raw: String): Long? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null

        val hasDot = cleaned.contains('.')
        val hasComma = cleaned.contains(',')

        val plain = when {
            hasDot && hasComma -> cleaned.replace(".", "").replace(',', '.')
            hasComma -> cleaned.replace(',', '.')
            hasDot -> {
                // "1.500" é milhar; "1.5" é decimal.
                if (Regex("""^\d{1,3}(\.\d{3})+$""").matches(cleaned)) cleaned.replace(".", "")
                else cleaned
            }
            else -> cleaned
        }

        val value = plain.toDoubleOrNull() ?: return null
        if (value < 0) return null
        return (value * 100).roundToLong()
    }

    private fun parseDecimal(raw: String): Long? = parseBrazilianAmount(raw)
}
