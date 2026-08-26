package br.com.financas.core.data.export

import br.com.financas.core.model.Category
import br.com.financas.core.model.Transaction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Exportação manual em CSV (§12) — sem nenhuma dependência de rede. */
object CsvExporter {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun export(transactions: List<Transaction>, categories: Map<String, Category>, zone: ZoneId = ZoneId.systemDefault()): String {
        val header = "Data,Descricao,Categoria,Tipo,Valor,FormaDePagamento\n"
        val rows = transactions.joinToString("\n") { tx ->
            val date = Instant.ofEpochMilli(tx.occurredAt).atZone(zone).format(DATE_FORMAT)
            val category = categories[tx.categoryId]?.name.orEmpty()
            val amount = "%.2f".format(tx.amountCents / 100.0)
            listOf(date, tx.description, category, tx.type.name, amount, tx.paymentMethod?.name.orEmpty())
                .joinToString(",") { field -> csvField(field) }
        }
        return header + rows
    }

    private fun csvField(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
}
