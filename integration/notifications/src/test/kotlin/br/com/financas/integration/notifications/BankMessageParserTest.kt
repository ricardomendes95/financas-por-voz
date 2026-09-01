package br.com.financas.integration.notifications

import br.com.financas.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BankMessageParserTest {

    @Test
    fun `compra aprovada extrai valor e estabelecimento`() {
        val result = BankMessageParser.parse("Compra aprovada: R\$ 45,90 em IFOOD SAO PAULO")
        assertNotNull(result)
        assertEquals(4590L, result!!.amountCents)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("IFOOD SAO PAULO", result.merchantRaw)
    }

    @Test
    fun `pix recebido gera receita`() {
        val result = BankMessageParser.parse("Você recebeu um Pix recebido de R\$ 100,00")
        assertNotNull(result)
        assertEquals(10_000L, result!!.amountCents)
        assertEquals(TransactionType.INCOME, result.type)
    }

    @Test
    fun `pix enviado gera despesa`() {
        val result = BankMessageParser.parse("Pix enviado no valor de R\$ 20,00 realizado com sucesso")
        assertNotNull(result)
        assertEquals(2_000L, result!!.amountCents)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    @Test
    fun `transferencia recebida sem mencionar pix gera receita`() {
        // Formato real do Nubank: "Recebemos sua transferência de R$ X,XX."
        val result = BankMessageParser.parse("Recebemos sua transferência de R\$ 1.400,00.")
        assertNotNull(result)
        assertEquals(140_000L, result!!.amountCents)
        assertEquals(TransactionType.INCOME, result.type)
    }

    @Test
    fun `transferencia recebida do nubank com titulo separado do corpo gera receita e captura remetente`() {
        // Formato real: notificação com título "Transferência recebida" e
        // corpo "Você recebeu uma transferência de\nR$ 100,00 de FULANO.".
        val result = BankMessageParser.parse(
            "Transferência recebida\nVocê recebeu uma transferência de\nR\$ 100,00 de REGIA PATRICIA DE MORAES VILAR."
        )
        assertNotNull(result)
        assertEquals(10_000L, result!!.amountCents)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals("Pix recebido de REGIA PATRICIA DE MORAES VILAR", result.merchantRaw)
    }

    @Test
    fun `transferencia enviada sem mencionar pix gera despesa`() {
        val result = BankMessageParser.parse("Transferência enviada: R\$ 50,00 para Fulano de Tal")
        assertNotNull(result)
        assertEquals(5_000L, result!!.amountCents)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    @Test
    fun `compra no debito do nubank sem a palavra aprovada junto do valor`() {
        // Formato real: título "Compra no débito aprovada" + corpo "Compra de R$ X em Y." —
        // a palavra "aprovada" nunca fica na mesma linha do valor.
        val result = BankMessageParser.parse("Compra de R\$ 5,50 em 99* 99*.")
        assertNotNull(result)
        assertEquals(550L, result!!.amountCents)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("99* 99*", result.merchantRaw)
    }

    @Test
    fun `compra no debito com titulo e corpo concatenados`() {
        val result = BankMessageParser.parse("Compra no débito aprovada\nCompra de R\$ 5,50 em 99* 99*.")
        assertNotNull(result)
        assertEquals(550L, result!!.amountCents)
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    @Test
    fun `debito generico`() {
        val result = BankMessageParser.parse("Débito de R\$ 32,80 em FARMACIA DROGASIL")
        assertNotNull(result)
        assertEquals(3_280L, result!!.amountCents)
    }

    @Test
    fun `texto sem padrao reconhecido retorna nulo`() {
        assertNull(BankMessageParser.parse("Seu extrato mensal já está disponível"))
    }

    @Test
    fun `normalizador remove prefixo de adquirente e numero de pedido`() {
        assertEquals("PEDIDO IFOOD", MerchantNormalizer.normalize("IFD*PEDIDO IFOOD 44219981"))
        assertEquals("NETFLIX", MerchantNormalizer.normalize("NETFLIX"))
    }
}
