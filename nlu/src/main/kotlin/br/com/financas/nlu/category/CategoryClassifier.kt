package br.com.financas.nlu.category

import br.com.financas.nlu.model.CategoryRule
import br.com.financas.nlu.model.ParseContext
import br.com.financas.nlu.model.TransactionType
import br.com.financas.nlu.text.Levenshtein
import br.com.financas.nlu.text.TextNormalizer

/**
 * Escolhe a categoria a partir da descrição.
 *
 * Cascata de tentativas, da mais confiável para a menos. Para na primeira que
 * bater — não há votação nem soma de scores, porque um empate mal resolvido é
 * pior que um "Outros" honesto marcado para revisão.
 */
object CategoryClassifier {

    private const val FUZZY_THRESHOLD = 0.25
    private const val MIN_FUZZY_LENGTH = 5

    data class Result(
        val categoryId: String,
        val confidence: Float,
        val trace: String
    )

    fun classify(
        description: String,
        type: TransactionType,
        context: ParseContext
    ): Result {
        val fallback = if (type == TransactionType.INCOME) {
            context.fallbackIncomeCategoryId
        } else {
            context.fallbackExpenseCategoryId
        }

        if (description.isBlank()) {
            return Result(fallback, 0.30f, "descrição vazia: caiu no fallback")
        }

        val normalized = TextNormalizer.normalizeKeyword(description)
        val words = normalized.split(" ").filter { it.isNotBlank() }
        val eligible = context.rules.filter { rule ->
            val category = context.categories.firstOrNull { it.id == rule.categoryId }
            category != null && (category.type == null || category.type == type)
        }

        // 1 e 2. Regra do usuário, depois regra de fábrica — match exato.
        exactMatch(normalized, words, eligible.filter { it.userDefined })?.let {
            return Result(it.categoryId, 1.0f, "regra do usuário: '${it.keyword}'")
        }
        exactMatch(normalized, words, eligible.filterNot { it.userDefined })?.let {
            return Result(it.categoryId, 0.90f, "regra de fábrica: '${it.keyword}'")
        }

        // 3. Prefixo — pega "mercadinho" a partir de "mercado", "farmacinha" de "farmacia".
        prefixMatch(words, eligible)?.let {
            return Result(it.categoryId, 0.75f, "match por radical: '${it.keyword}'")
        }

        // 4. Fuzzy — absorve erro de transcrição ("netiflix", "pastél").
        fuzzyMatch(words, eligible)?.let { (rule, word) ->
            return Result(rule.categoryId, 0.65f, "match aproximado: '$word' ≈ '${rule.keyword}'")
        }

        // 5. Nome da própria categoria dito na frase.
        context.categories
            .filter { it.type == null || it.type == type }
            .firstOrNull { category ->
                val name = TextNormalizer.normalizeKeyword(category.name)
                words.any { it == name } || normalized.contains(name)
            }
            ?.let { return Result(it.id, 0.85f, "nome da categoria citado: '${it.name}'") }

        return Result(fallback, 0.30f, "nenhuma regra casou: caiu no fallback")
    }

    private fun exactMatch(
        normalized: String,
        words: List<String>,
        rules: List<CategoryRule>
    ): CategoryRule? = rules
        .filter { rule ->
            if (rule.keyword.contains(' ')) normalized.contains(rule.keyword)
            else words.any { it == rule.keyword }
        }
        .maxByOrNull { it.weight }

    private fun prefixMatch(words: List<String>, rules: List<CategoryRule>): CategoryRule? = rules
        .filter { rule ->
            rule.keyword.length >= 4 && words.any { word ->
                word.length >= 4 &&
                    (word.startsWith(rule.keyword.take(5)) || rule.keyword.startsWith(word.take(5)))
            }
        }
        .maxByOrNull { it.weight }

    private fun fuzzyMatch(
        words: List<String>,
        rules: List<CategoryRule>
    ): Pair<CategoryRule, String>? {
        var best: Pair<CategoryRule, String>? = null
        var bestDistance = FUZZY_THRESHOLD

        for (word in words) {
            if (word.length < MIN_FUZZY_LENGTH) continue
            for (rule in rules) {
                if (rule.keyword.length < MIN_FUZZY_LENGTH) continue
                val distance = Levenshtein.normalized(word, rule.keyword)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = rule to word
                }
            }
        }
        return best
    }

    /**
     * Extrai a palavra-chave mais significativa de uma descrição, para criar
     * uma regra de aprendizado quando o usuário corrige a categoria à mão.
     * Devolve null se não houver candidata boa o bastante.
     */
    fun suggestKeyword(description: String): String? {
        val words = TextNormalizer.normalizeKeyword(description)
            .split(" ")
            .filter { it.length >= 4 && it.any(Char::isLetter) }
        return words.maxByOrNull { it.length }
    }
}
