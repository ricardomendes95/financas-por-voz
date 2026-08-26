package br.com.financas.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MoneyFormatterTest {

    @Test
    fun `parseToCents aceita virgula brasileira`() {
        assertEquals(2050L, MoneyFormatter.parseToCents("20,50"))
    }

    @Test
    fun `parseToCents aceita numero inteiro`() {
        assertEquals(2000L, MoneyFormatter.parseToCents("20"))
    }

    @Test
    fun `parseToCents aceita milhar com ponto e centavos com virgula`() {
        assertEquals(123456L, MoneyFormatter.parseToCents("1.234,56"))
    }

    @Test
    fun `parseToCents rejeita texto invalido`() {
        assertNull(MoneyFormatter.parseToCents("abc"))
    }

    @Test
    fun `parseToCents rejeita vazio`() {
        assertNull(MoneyFormatter.parseToCents(""))
    }
}
