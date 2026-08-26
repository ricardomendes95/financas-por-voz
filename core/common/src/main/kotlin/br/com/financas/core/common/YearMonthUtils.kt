package br.com.financas.core.common

import java.time.Instant
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Deriva as colunas desnormalizadas `yearMonth` (formato `YYYYMM`) e
 * `dayOfWeek` (1..7, ISO) a partir de epoch millis — usado ao gravar um
 * lançamento, nunca em consulta (regra §3: consultas mensais não usam
 * funções de data no SQL).
 */
object YearMonthUtils {

    private val ptBR: Locale = Locale.Builder().setLanguage("pt").setRegion("BR").build()

    fun yearMonthOf(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return date.year * 100 + date.monthValue
    }

    fun dayOfWeekOf(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Int =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().dayOfWeek.value

    fun currentYearMonth(zone: ZoneId = ZoneId.systemDefault()): Int =
        yearMonthOf(System.currentTimeMillis(), zone)

    fun plusMonths(yearMonth: Int, delta: Long): Int {
        val ym = YearMonth.of(yearMonth / 100, yearMonth % 100).plusMonths(delta)
        return ym.year * 100 + ym.monthValue
    }

    /** Ex.: "Agosto 2026". */
    fun fullMonthLabel(yearMonth: Int): String {
        val name = Month.of(yearMonth % 100).getDisplayName(TextStyle.FULL, ptBR).replaceFirstChar { it.uppercase() }
        return "$name ${yearMonth / 100}"
    }

    /** Ex.: "Ago/2026". */
    fun shortMonthLabel(yearMonth: Int): String {
        val name = Month.of(yearMonth % 100).getDisplayName(TextStyle.SHORT, ptBR).replaceFirstChar { it.uppercase() }
        return "$name/${yearMonth / 100}"
    }
}
