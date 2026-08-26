package br.com.financas.integration.notifications

import br.com.financas.core.model.TransactionType

data class BankMessageResult(
    val amountCents: Long,
    val type: TransactionType,
    val merchantRaw: String
)

/**
 * Reconhece os três formatos mais comuns de notificação bancária (§8.3).
 * Roda em cima do texto original (com acentos e caixa preservados) — só a
 * comparação dos padrões ignora caixa.
 */
object BankMessageParser {

    private val PURCHASE_APPROVED = Regex(
        """compra\s+aprovada.*?R\$\s*([\d.,]+)\s*(?:em|no|na)\s+(.+?)(?:[.\n]|$)""",
        RegexOption.IGNORE_CASE
    )

    private val PIX = Regex(
        """pix\s+(enviado|recebido).*?R\$\s*([\d.,]+)""",
        RegexOption.IGNORE_CASE
    )

    private val DEBIT = Regex(
        """(?:d[ée]bito|debitado).*?R\$\s*([\d.,]+)\s*(?:em|no|na)?\s*(.+?)?(?:[.\n]|$)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(text: String): BankMessageResult? {
        PURCHASE_APPROVED.find(text)?.let { match ->
            val cents = parseAmount(match.groupValues[1]) ?: return@let
            return BankMessageResult(cents, TransactionType.EXPENSE, match.groupValues[2].trim())
        }

        PIX.find(text)?.let { match ->
            val cents = parseAmount(match.groupValues[2]) ?: return@let
            val type = if (match.groupValues[1].equals("recebido", ignoreCase = true)) {
                TransactionType.INCOME
            } else {
                TransactionType.EXPENSE
            }
            return BankMessageResult(cents, type, "Pix")
        }

        DEBIT.find(text)?.let { match ->
            val cents = parseAmount(match.groupValues[1]) ?: return@let
            val merchant = match.groupValues.getOrNull(2)?.trim().orEmpty().ifBlank { "Débito" }
            return BankMessageResult(cents, TransactionType.EXPENSE, merchant)
        }

        return null
    }

    private fun parseAmount(raw: String): Long? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null
        val normalized = if (cleaned.contains(',')) {
            cleaned.replace(".", "").replace(',', '.')
        } else {
            cleaned
        }
        val value = normalized.toDoubleOrNull() ?: return null
        if (value <= 0) return null
        return Math.round(value * 100)
    }
}
