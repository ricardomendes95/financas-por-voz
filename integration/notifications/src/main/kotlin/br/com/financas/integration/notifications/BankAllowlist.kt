package br.com.financas.integration.notifications

/** Bancos sugeridos (§8.2) — o usuário marca quais ativar, nenhum vem ligado por padrão. */
data class BankApp(val displayName: String, val packageName: String)

object BankAllowlist {
    val KNOWN_BANKS: List<BankApp> = listOf(
        BankApp("Nubank", "com.nu.production"),
        BankApp("Inter", "br.com.intermedium"),
        BankApp("Itaú", "com.itau"),
        BankApp("Bradesco", "com.bradesco"),
        BankApp("Santander", "com.santander.app"),
        BankApp("Banco do Brasil", "br.com.bb.android"),
        BankApp("Caixa", "com.caixa.gov.br"),
        BankApp("C6 Bank", "com.c6bank.app"),
        BankApp("PicPay", "com.picpay"),
        BankApp("Mercado Pago", "com.mercadopago.wallet"),
        BankApp("PagBank", "br.com.uol.ps.myaccount"),
        BankApp("Neon", "com.neon.pf"),
        BankApp("Will Bank", "com.willbank.app")
    )
}
