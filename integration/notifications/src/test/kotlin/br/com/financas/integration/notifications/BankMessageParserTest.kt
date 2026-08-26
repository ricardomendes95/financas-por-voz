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
