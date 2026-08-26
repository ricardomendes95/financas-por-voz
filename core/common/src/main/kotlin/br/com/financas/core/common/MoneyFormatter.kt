package br.com.financas.core.common

import java.text.NumberFormat
import java.util.Locale

/** Formata `Long` centavos como moeda pt-BR. Nunca usa `Double`/`Float`. */
object MoneyFormatter {

    private fun currencyInstance(): NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("pt").setRegion("BR").build())

    /** "R$ 20,50" a partir de 2050. */
    fun format(cents: Long): String = currencyInstance().format(cents / 100.0)

    /** "−R$ 20,50" para despesa, "+R$ 20,50" para receita — nunca só a cor (acessibilidade). */
    fun formatSigned(cents: Long, isExpense: Boolean): String {
        val sign = if (isExpense) "−" else "+"
        return "$sign${format(cents.coerceAtLeast(0))}"
    }

    /**
     * Forma abreviada para gráficos: "R$ 12,4 mil" a partir de R$ 10.000+.
     * Nunca usar em listas — só em gráficos/cards, conforme §10.3.
     */
    fun formatAbbreviated(cents: Long): String {
        val reais = cents / 100.0
        return if (reais >= 10_000) {
            "R$ %.1f mil".format(reais / 1000.0).replace('.', ',')
        } else {
            format(cents)
        }
    }

    /** 2050 → "20,50" — sem símbolo de moeda, para pré-preencher campos de formulário editáveis. */
    fun formatPlain(cents: Long): String = "%.2f".format(cents / 100.0).replace('.', ',')

    /** "20,50" ou "20.50" ou "20" → 2050 centavos. `null` se não for um número válido. */
    fun parseToCents(text: String): Long? {
        val cleaned = text.trim().replace("R$", "").trim()
        if (cleaned.isEmpty()) return null
        val normalized = cleaned.replace(".", "").replace(',', '.')
        val value = normalized.toDoubleOrNull() ?: return null
        if (value < 0) return null
        return Math.round(value * 100)
    }
}
