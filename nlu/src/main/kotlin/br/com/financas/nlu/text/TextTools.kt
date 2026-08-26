package br.com.financas.nlu.text

/**
 * Normalização que **preserva os índices** caractere a caractere.
 *
 * Isso é o que permite que cada extrator marque as posições que consumiu no
 * texto normalizado e que, no fim, o extrator de descrição recorte exatamente
 * as mesmas posições do texto ORIGINAL. Por isso nada aqui pode alterar o
 * comprimento da string: sem colapsar espaços, sem expandir contrações.
 */
object TextNormalizer {

    private const val ACCENTED =
        "áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ"
    private const val PLAIN =
        "aaaaaeeeeiiiiooooouuuucnaaaaaeeeeiiiiooooouuuucn"

    /** Caracteres mantidos por serem semanticamente relevantes em valores/datas. */
    private val KEPT = setOf(',', '.', '/', '$', '-')

    fun normalize(input: String): String {
        require(ACCENTED.length == PLAIN.length) { "tabelas de acento dessincronizadas" }
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val idx = ACCENTED.indexOf(ch)
            val c = if (idx >= 0) PLAIN[idx] else ch.lowercaseChar()
            sb.append(if (c.isLetterOrDigit() || c in KEPT) c else ' ')
        }
        check(sb.length == input.length) { "normalização alterou o comprimento" }
        return sb.toString()
    }

    /** Versão para comparar palavras-chave — aqui pode colapsar à vontade. */
    fun normalizeKeyword(input: String): String =
        normalize(input).replace(Regex("\\s+"), " ").trim()
}

/** Palavra com sua posição no texto normalizado. `end` é exclusivo. */
data class Token(val text: String, val start: Int, val end: Int) {
    val range: IntRange get() = start until end
}

object Tokenizer {
    private val WORD = Regex("[\\p{L}\\p{N}][\\p{L}\\p{N},./$-]*")

    fun tokenize(normalized: String): List<Token> =
        WORD.findAll(normalized).map { Token(it.value, it.range.first, it.range.last + 1) }.toList()
}

/**
 * Marca as regiões do texto já interpretadas por algum extrator.
 *
 * A ordem do pipeline importa: data é extraída antes do valor justamente para
 * que "no dia 24" não seja confundido com um valor de R$ 24.
 */
class ConsumptionMask(private val length: Int) {

    private val consumed = BooleanArray(length)

    fun consume(range: IntRange) {
        for (i in range) if (i in 0 until length) consumed[i] = true
    }

    fun consume(start: Int, endExclusive: Int) = consume(start until endExclusive)

    fun isConsumed(index: Int): Boolean = index in 0 until length && consumed[index]

    fun overlaps(range: IntRange): Boolean = range.any { isConsumed(it) }

    /** Texto com as regiões consumidas substituídas por espaço. */
    fun blankOut(source: String): String {
        val sb = StringBuilder(source)
        for (i in 0 until minOf(length, source.length)) if (consumed[i]) sb[i] = ' '
        return sb.toString()
    }
}

/** Distância de Levenshtein normalizada (0.0 = idêntico, 1.0 = totalmente diferente). */
object Levenshtein {

    fun distance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + cost
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }

    fun normalized(a: String, b: String): Double {
        val longest = maxOf(a.length, b.length)
        return if (longest == 0) 0.0 else distance(a, b).toDouble() / longest
    }
}
