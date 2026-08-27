package br.com.financas.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** "Hoje", "Ontem", "Seg, 24 ago" — nunca `24/08/2026` em lista, conforme §10.3. */
object RelativeDateFormatter {

    private val PT_BR = Locale.Builder().setLanguage("pt").setRegion("BR").build()
    private val DAY_MONTH = DateTimeFormatter.ofPattern("d 'de' MMM", PT_BR)
    private val HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm", PT_BR)

    /** "08:25" — hora exata em que o lançamento ocorreu (voz/notificação) ou foi editado manualmente. */
    fun time(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime().format(HOUR_MINUTE)

    fun format(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return when {
            date == today -> "Hoje"
            date == today.minusDays(1) -> "Ontem"
            date == today.plusDays(1) -> "Amanhã"
            else -> {
                val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, PT_BR)
                    .replaceFirstChar { it.uppercase() }
                    .removeSuffix(".")
                "$weekday, ${date.format(DAY_MONTH)}"
            }
        }
    }

    /** Cabeçalho sticky de grupo: "HOJE", "ONTEM", "24 DE AGOSTO". */
    fun sectionHeader(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return when {
            date == today -> "HOJE"
            date == today.minusDays(1) -> "ONTEM"
            else -> date.format(DAY_MONTH).uppercase(PT_BR)
        }
    }
}
