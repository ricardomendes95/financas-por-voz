package br.com.financas.nlu.extract

import br.com.financas.nlu.model.PaymentMethod
import br.com.financas.nlu.model.TransactionType
import br.com.financas.nlu.text.ConsumptionMask

/** Decide se o lançamento é entrada ou saída de dinheiro. */
object TypeExtractor {

    private val INCOME = Regex(
        """\b(recebi|receber|recebido|recebimento|entrou|entrada|ganhei|ganho|caiu|creditou|credito de|receita|salario|vendi|venda|me pagaram|reembolso|estorno|estornou)\b"""
    )

    private val EXPENSE = Regex(
        """\b(gastei|gasto|gastar|paguei|pagar|comprei|comprar|compra|despesa|saiu|saida|torrei|debitou|sai)\b"""
    )

    data class Result(val type: TransactionType, val confidence: Float, val trace: String)

    fun extract(normalized: String, mask: ConsumptionMask): Result {
        val income = INCOME.find(normalized)?.takeIf { !mask.overlaps(it.range) }
        val expense = EXPENSE.find(normalized)?.takeIf { !mask.overlaps(it.range) }

        return when {
            // Quando os dois aparecem, o que vem primeiro na frase manda.
            income != null && expense != null -> {
                if (income.range.first < expense.range.first) {
                    mask.consume(income.range)
                    Result(TransactionType.INCOME, 0.80f, "entrada e saída presentes; entrada veio antes")
                } else {
                    mask.consume(expense.range)
                    Result(TransactionType.EXPENSE, 0.80f, "entrada e saída presentes; saída veio antes")
                }
            }

            income != null -> {
                mask.consume(income.range)
                Result(TransactionType.INCOME, 1.0f, "verbo de entrada: '${income.value}'")
            }

            expense != null -> {
                mask.consume(expense.range)
                Result(TransactionType.EXPENSE, 1.0f, "verbo de saída: '${expense.value}'")
            }

            // Sem verbo: a esmagadora maioria dos lançamentos é despesa.
            else -> Result(TransactionType.EXPENSE, 0.65f, "tipo ausente: assumiu despesa")
        }
    }
}

/** Detecta a forma de pagamento apenas quando dita explicitamente. */
object PaymentMethodExtractor {

    private val PATTERNS: List<Pair<Regex, PaymentMethod>> = listOf(
        Regex("""\bpix\b""") to PaymentMethod.PIX,
        Regex("""\b(credito|cartao de credito|no cartao|parcelado|parcelei)\b""") to PaymentMethod.CREDIT,
        Regex("""\b(debito|cartao de debito)\b""") to PaymentMethod.DEBIT,
        Regex("""\b(dinheiro|especie|a vista|em cash)\b""") to PaymentMethod.CASH,
        Regex("""\bboleto\b""") to PaymentMethod.BOLETO,
        Regex("""\b(transferencia|ted|doc)\b""") to PaymentMethod.TRANSFER
    )

    data class Result(val method: PaymentMethod, val trace: String)

    fun extract(normalized: String, mask: ConsumptionMask): Result? {
        for ((regex, method) in PATTERNS) {
            val match = regex.find(normalized) ?: continue
            if (mask.overlaps(match.range)) continue
            mask.consume(match.range)
            return Result(method, "forma de pagamento: ${match.value}")
        }
        return null
    }
}

/**
 * Monta a descrição com o que sobrou depois que todos os outros extratores
 * consumiram suas partes.
 */
object DescriptionExtractor {

    private val ACTION_VERBS = setOf(
        "adicione", "adicionar", "adiciona", "add", "registre", "registrar",
        "registra", "lance", "lancar", "lanca", "anote", "anotar", "anota",
        "coloque", "colocar", "bota", "botar", "poe", "por", "cria", "criar",
        "marque", "marcar", "salve", "salvar", "insira", "inserir"
    )

    private val STOPWORDS = setOf(
        "de", "do", "da", "dos", "das", "em", "no", "na", "nos", "nas",
        "num", "numa", "nuns", "numas", "com", "para", "pra", "pro", "por",
        "o", "a", "os", "as", "um", "uma", "uns", "umas", "e", "que",
        "meu", "minha", "meus", "minhas", "ao", "aos", "a", "as", "se",
        "foi", "era", "esse", "essa", "isso", "aqui", "la", "reais", "real"
    )

    /**
     * @param original texto como o usuário falou, para preservar acentos e caixa
     * @param normalized versão normalizada, mesmo comprimento
     */
    fun extract(original: String, normalized: String, mask: ConsumptionMask): String {
        val remainingNormalized = mask.blankOut(normalized)

        val keptRanges = mutableListOf<IntRange>()
        Regex("""[\p{L}\p{N}][\p{L}\p{N}'-]*""").findAll(remainingNormalized).forEach { match ->
            val word = match.value
            if (word in ACTION_VERBS) return@forEach
            if (word in STOPWORDS) return@forEach
            if (word.length == 1 && !word[0].isDigit()) return@forEach
            keptRanges += match.range
        }

        if (keptRanges.isEmpty()) return ""

        val words = keptRanges.map { range ->
            original.substring(range.first, minOf(range.last + 1, original.length))
        }

        return words.joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .replaceFirstChar { it.uppercase() }
    }
}
