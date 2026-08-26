package br.com.financas.core.data.statement

import br.com.financas.core.model.StatementEntry
import br.com.financas.core.model.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Extrato exportado do Nubank em CSV: `Data,Valor,Identificador,Descrição`,
 * data `DD/MM/AAAA`, valor com ponto decimal (negativo = saída, positivo =
 * entrada), identificador um UUID único por transação — usado como chave de
 * dedup (§5.9/§12). Linhas que não batem com esse formato são ignoradas: um
 * extrato bancário real não tem linha pra "corrigir", só cabeçalho e dados.
 */
object NubankCsvStatementParser {

    const val SOURCE = "nubank_csv"

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun parse(content: String, zone: ZoneId = ZoneId.systemDefault()): List<StatementEntry> =
        content.lineSequence()
            .drop(1) // cabeçalho "Data,Valor,Identificador,Descrição"
            .mapNotNull { line -> parseLine(line, zone) }
            .toList()

    private fun parseLine(line: String, zone: ZoneId): StatementEntry? {
        if (line.isBlank()) return null
        val parts = line.split(",", limit = 4)
        if (parts.size < 4) return null

        val date = runCatching { LocalDate.parse(parts[0].trim(), dateFormatter) }.getOrNull() ?: return null
        val amount = parts[1].trim().toDoubleOrNull() ?: return null
        val identifier = parts[2].trim()
        if (identifier.isBlank()) return null
        val description = parts[3].trim()

        val cents = Math.round(kotlin.math.abs(amount) * 100)
        return StatementEntry(
            externalId = "$SOURCE:$identifier",
            occurredAt = date.atStartOfDay(zone).toInstant().toEpochMilli(),
            amountCents = cents,
            type = if (amount < 0) TransactionType.EXPENSE else TransactionType.INCOME,
            description = description
        )
    }
}
