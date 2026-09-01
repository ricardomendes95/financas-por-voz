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

    // "Compra no débito aprovada" (título) + "Compra de R$ 5,50 em 99* 99*." (corpo) — o
    // Nubank não repete "aprovada" na mesma linha do valor, então PURCHASE_APPROVED não bate.
    // Mais genérica: qualquer "compra [de] R$ X em/no/na Y", sem exigir "aprovada" junto.
    private val PURCHASE_GENERIC = Regex(
        """compra\s+(?:de\s+)?R\$\s*([\d.,]+)\s*(?:em|no|na)\s+(.+?)(?:[.\n]|$)""",
        RegexOption.IGNORE_CASE
    )

    private val PIX = Regex(
        """pix\s+(enviado|recebido).*?R\$\s*([\d.,]+)""",
        RegexOption.IGNORE_CASE
    )

    // Muitos bancos (ex.: Nubank) notificam transferência recebida/enviada
    // sem citar "pix" em nenhum ponto do texto — ex.: "Recebemos sua
    // transferência de R$ 1.400,00." ou "Transferência enviada".
    private val TRANSFER_RECEIVED = Regex(
        """(?:transfer[êe]ncia\s+recebida|recebemos\s+sua\s+transfer[êe]ncia).*?R\$\s*([\d.,]+)""",
        RegexOption.IGNORE_CASE
    )

    // Formato real do Nubank para Pix recebido: título "Transferência
    // recebida" (sozinho) + corpo "Você recebeu uma transferência de\nR$
    // 100,00 de FULANO." — a palavra "recebida" só existe no título, e como
    // o listener junta título e corpo com "\n" (que "." não atravessa),
    // TRANSFER_RECEIVED não bate. Aqui casamos o corpo isolado, já
    // aproveitando para capturar quem enviou.
    private val TRANSFER_RECEIVED_WITH_SENDER = Regex(
        """recebeu\s+uma\s+transfer[êe]ncia\s+de\s*R\$\s*([\d.,]+)\s*de\s+(.+?)(?:[.\n]|$)""",
        RegexOption.IGNORE_CASE
    )
    private val TRANSFER_SENT = Regex(
        """(?:transfer[êe]ncia\s+enviada|enviamos\s+sua\s+transfer[êe]ncia).*?R\$\s*([\d.,]+)""",
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

        PURCHASE_GENERIC.find(text)?.let { match ->
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

        TRANSFER_RECEIVED_WITH_SENDER.find(text)?.let { match ->
            val cents = parseAmount(match.groupValues[1]) ?: return@let
            val sender = match.groupValues[2].trim()
            val merchant = if (sender.isBlank()) "Pix recebido" else "Pix recebido de $sender"
            return BankMessageResult(cents, TransactionType.INCOME, merchant)
        }

        TRANSFER_RECEIVED.find(text)?.let { match ->
            val cents = parseAmount(match.groupValues[1]) ?: return@let
            return BankMessageResult(cents, TransactionType.INCOME, "Transferência")
        }

        TRANSFER_SENT.find(text)?.let { match ->
            val cents = parseAmount(match.groupValues[1]) ?: return@let
            return BankMessageResult(cents, TransactionType.EXPENSE, "Transferência")
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
