package br.com.financas.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class YearMonthUtilsTest {

    private val zone = ZoneOffset.UTC
    private val aug24 = LocalDateTime.of(2026, 8, 24, 12, 0).toInstant(zone).toEpochMilli()

    @Test
    fun `yearMonth no formato YYYYMM`() {
        assertEquals(202608, YearMonthUtils.yearMonthOf(aug24, zone))
    }

    @Test
    fun `dayOfWeek ISO 1 a 7`() {
        // 24/08/2026 é segunda-feira.
        assertEquals(1, YearMonthUtils.dayOfWeekOf(aug24, zone))
    }
}
