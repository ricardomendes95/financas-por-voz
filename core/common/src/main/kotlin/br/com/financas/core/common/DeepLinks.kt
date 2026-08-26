package br.com.financas.core.common

/**
 * URIs `financas://...` — rota 7 da entrada por voz (§5.8). Vivem aqui, num
 * módulo Kotlin puro, porque tanto `:feature:voice` (que as monta em
 * `PendingIntent`s) quanto `:app` (que declara os `intent-filter` e navega)
 * precisam do mesmo vocabulário sem depender um do outro.
 */
object DeepLinks {
    const val SCHEME = "financas"

    const val HOST_ADD = "add"
    const val HOST_EDIT = "edit"
    const val HOST_CAPTURE = "capture"
    const val HOST_REPORT = "report"

    const val PARAM_TEXT = "text"
    const val PARAM_ID = "id"
    const val PARAM_MONTH = "month"
    const val PARAM_TYPE = "type"

    const val TYPE_EXPENSE = "expense"
    const val TYPE_INCOME = "income"

    fun add(text: String? = null, type: String? = null): String {
        val params = buildList {
            text?.let { add("$PARAM_TEXT=${encode(it)}") }
            type?.let { add("$PARAM_TYPE=${encode(it)}") }
        }
        return if (params.isEmpty()) "$SCHEME://$HOST_ADD" else "$SCHEME://$HOST_ADD?${params.joinToString("&")}"
    }

    fun edit(transactionId: String): String = "$SCHEME://$HOST_EDIT?$PARAM_ID=${encode(transactionId)}"

    fun capture(): String = "$SCHEME://$HOST_CAPTURE"

    fun report(yearMonth: String): String = "$SCHEME://$HOST_REPORT?$PARAM_MONTH=${encode(yearMonth)}"

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")
}
