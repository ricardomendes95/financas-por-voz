package br.com.financas.nlu.text

/**
 * Converte numerais por extenso em português brasileiro para valor inteiro.
 *
 * Cobre 0 a 999.999.999. Aceita a conjunção "e" nas posições idiomáticas
 * ("cento e vinte", "mil e duzentos", "dois mil e quinhentos").
 */
object PortugueseNumbers {

    private val UNITS: Map<String, Long> = mapOf(
        "zero" to 0, "um" to 1, "uma" to 1, "dois" to 2, "duas" to 2, "tres" to 3,
        "quatro" to 4, "cinco" to 5, "seis" to 6, "sete" to 7, "oito" to 8,
        "nove" to 9, "dez" to 10, "onze" to 11, "doze" to 12, "treze" to 13,
        "catorze" to 14, "quatorze" to 14, "quinze" to 15, "dezesseis" to 16,
        "dezessete" to 17, "dezoito" to 18, "dezenove" to 19, "vinte" to 20,
        "trinta" to 30, "quarenta" to 40, "cinquenta" to 50, "sessenta" to 60,
        "setenta" to 70, "oitenta" to 80, "noventa" to 90,
        "cem" to 100, "cento" to 100, "duzentos" to 200, "duzentas" to 200,
        "trezentos" to 300, "trezentas" to 300, "quatrocentos" to 400,
        "quatrocentas" to 400, "quinhentos" to 500, "quinhentas" to 500,
        "seiscentos" to 600, "seiscentas" to 600, "setecentos" to 700,
        "setecentas" to 700, "oitocentos" to 800, "oitocentas" to 800,
        "novecentos" to 900, "novecentas" to 900
    )

    private val SCALES: Map<String, Long> = mapOf(
        "mil" to 1_000L,
        "milhao" to 1_000_000L,
        "milhoes" to 1_000_000L,
        "bilhao" to 1_000_000_000L,
        "bilhoes" to 1_000_000_000L
    )

    /** true se a palavra pode fazer parte de um numeral por extenso. */
    fun isNumberWord(word: String): Boolean =
        word in UNITS || word in SCALES

    /** true para a conjunção que liga partes de um numeral. */
    fun isConnector(word: String): Boolean = word == "e"

    /**
     * Converte uma sequência de palavras num número.
     * Retorna null se a sequência não formar um numeral válido.
     */
    fun parse(words: List<String>): Long? {
        if (words.isEmpty()) return null

        var total = 0L
        var current = 0L
        var sawAnyNumber = false
        var lastWasConnector = false

        for (word in words) {
            when {
                isConnector(word) -> {
                    // "e" solto no início ou duplicado invalida a sequência
                    if (!sawAnyNumber || lastWasConnector) return null
                    lastWasConnector = true
                    continue
                }

                word in UNITS -> {
                    current += UNITS.getValue(word)
                    sawAnyNumber = true
                }

                word in SCALES -> {
                    val scale = SCALES.getValue(word)
                    // "mil" sozinho vale 1000; "dois mil" vale 2000
                    val multiplier = if (current == 0L) 1L else current
                    total += multiplier * scale
                    current = 0L
                    sawAnyNumber = true
                }

                else -> return null
            }
            lastWasConnector = false
        }

        if (!sawAnyNumber || lastWasConnector) return null
        return total + current
    }

    /**
     * Varre os tokens a partir de [startIndex] e devolve o maior trecho
     * consecutivo que forma um numeral, junto com o índice do primeiro token
     * fora dele.
     *
     * Conectores "e" só são absorvidos quando seguidos de outra palavra
     * numérica — assim "vinte reais e cinquenta centavos" não engole o "e".
     */
    fun consumeRun(tokens: List<Token>, startIndex: Int): NumberRun? {
        if (startIndex >= tokens.size) return null
        if (!isNumberWord(tokens[startIndex].text)) return null

        var index = startIndex
        val words = mutableListOf<String>()

        while (index < tokens.size) {
            val word = tokens[index].text
            if (isNumberWord(word)) {
                words += word
                index++
            } else if (isConnector(word) &&
                index + 1 < tokens.size &&
                isNumberWord(tokens[index + 1].text)
            ) {
                words += word
                index++
            } else {
                break
            }
        }

        val value = parse(words) ?: return null
        return NumberRun(
            value = value,
            firstTokenIndex = startIndex,
            endTokenIndexExclusive = index,
            range = tokens[startIndex].start until tokens[index - 1].end
        )
    }

    data class NumberRun(
        val value: Long,
        val firstTokenIndex: Int,
        val endTokenIndexExclusive: Int,
        val range: IntRange
    )
}
