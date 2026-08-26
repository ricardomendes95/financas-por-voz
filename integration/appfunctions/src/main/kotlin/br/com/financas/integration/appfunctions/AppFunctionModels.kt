package br.com.financas.integration.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/**
 * Resumo lido de volta ao usuário pelo agente depois de um lançamento
 * (§5.2).
 *
 * @param success Se o lançamento foi de fato gravado.
 * @param summary Texto curto, falável, com o resultado.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class EntryResult(
    val success: Boolean,
    val summary: String
)

/**
 * Resposta a uma pergunta sobre gastos (§7.5).
 *
 * @param summary Resposta curta e falável.
 * @param totalCents Total gasto no mês, em centavos.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SpendingSummary(
    val summary: String,
    val totalCents: Long
)

/**
 * Categoria recém-criada.
 *
 * @param categoryId Identificador da categoria criada.
 * @param name Nome da categoria.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class CategoryResult(
    val categoryId: String,
    val name: String
)
