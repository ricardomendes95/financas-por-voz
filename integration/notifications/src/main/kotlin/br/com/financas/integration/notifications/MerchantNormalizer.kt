package br.com.financas.integration.notifications

/**
 * "IFD *PEDIDO 4421" → "PEDIDO" seria ruído; removemos prefixo de adquirente
 * e números de pedido/cidade, sobrando o nome do estabelecimento (§8.3).
 */
object MerchantNormalizer {

    private val ACQUIRER_PREFIXES = listOf("PAG*", "MP*", "IFD*", "PICPAY*", "PIX*", "MERCADOPAGO*")
    private val TRAILING_NUMBERS = Regex("""\s*\d{3,}\s*$""")
    private val LEADING_NUMBERS = Regex("""^\s*\d{3,}\s*""")
    private val EXTRA_SPACES = Regex("""\s+""")

    fun normalize(raw: String): String {
        var result = raw.trim().uppercase()
        for (prefix in ACQUIRER_PREFIXES) {
            if (result.startsWith(prefix)) {
                result = result.removePrefix(prefix)
                break
            }
        }
        result = result.replace(TRAILING_NUMBERS, "")
        result = result.replace(LEADING_NUMBERS, "")
        result = result.replace(EXTRA_SPACES, " ").trim()
        return result.ifBlank { raw.trim() }
    }
}
