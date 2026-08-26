package br.com.financas.nlu.extract

import br.com.financas.nlu.model.DatePrecision
import br.com.financas.nlu.text.ConsumptionMask
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * Resolve expressões temporais em pt-BR.
 *
 * Executa **antes** do extrator de valor, para que "no dia 24" nunca seja
 * confundido com um valor monetário. Números soltos jamais são tratados como
 * data: é obrigatório haver uma âncora ("dia", separador de data, nome de mês
 * ou nome de dia da semana).
 */
object DateExtractor {

    /** Hora neutra usada quando a data não é hoje. */
    private val NEUTRAL_TIME: LocalTime = LocalTime.of(12, 0)

    private val MONTHS: Map<String, Int> = mapOf(
        "janeiro" to 1, "jan" to 1,
        "fevereiro" to 2, "fev" to 2,
        "marco" to 3, "mar" to 3,
        "abril" to 4, "abr" to 4,
        "maio" to 5, "mai" to 5,
        "junho" to 6, "jun" to 6,
        "julho" to 7, "jul" to 7,
        "agosto" to 8, "ago" to 8,
        "setembro" to 9, "set" to 9,
        "outubro" to 10, "out" to 10,
        "novembro" to 11, "nov" to 11,
        "dezembro" to 12, "dez" to 12
    )

    private val WEEKDAYS: Map<String, DayOfWeek> = mapOf(
        "segunda" to DayOfWeek.MONDAY,
        "terca" to DayOfWeek.TUESDAY,
        "quarta" to DayOfWeek.WEDNESDAY,
        "quinta" to DayOfWeek.THURSDAY,
        "sexta" to DayOfWeek.FRIDAY,
        "sabado" to DayOfWeek.SATURDAY,
        "domingo" to DayOfWeek.SUNDAY
    )

    private val FUTURE_MARKER = Regex("""\b(amanha|que vem|proxim[ao]|semana que vem)\b""")

    data class Result(
        val dateTime: LocalDateTime,
        val precision: DatePrecision,
        val trace: String
    )

    fun extract(normalized: String, now: LocalDateTime, mask: ConsumptionMask): Result {
        val hasFutureMarker = FUTURE_MARKER.containsMatchIn(normalized)

        // Ordem importa: padrões mais específicos primeiro.
        resolveDayMonthYearWords(normalized, now, mask)?.let { return it }
        resolveNumericDate(normalized, now, mask)?.let { return it }
        resolveExplicitDay(normalized, now, mask, hasFutureMarker)?.let { return it }
        resolveDaysAgo(normalized, now, mask)?.let { return it }
        resolveWeekday(normalized, now, mask, hasFutureMarker)?.let { return it }
        resolveKeyword(normalized, now, mask)?.let { return it }

        return Result(now, DatePrecision.ASSUMED_NOW, "data ausente: assumiu agora")
    }

    // ---- "dia 24 de julho", "24 de julho de 2026" ----------------------------

    private val DAY_MONTH_WORDS = Regex(
        """\b(?:dia\s+)?(\d{1,2})\s+de\s+([a-z]{3,9})(?:\s+de\s+(\d{4}))?\b"""
    )

    private fun resolveDayMonthYearWords(
        text: String,
        now: LocalDateTime,
        mask: ConsumptionMask
    ): Result? {
        for (match in DAY_MONTH_WORDS.findAll(text)) {
            if (mask.overlaps(match.range)) continue
            val month = MONTHS[match.groupValues[2]] ?: continue
            val day = match.groupValues[1].toIntOrNull() ?: continue
            val year = match.groupValues[3].toIntOrNull() ?: now.year
            val date = safeDate(year, month, day) ?: continue
            mask.consume(match.range)
            return Result(
                date.atTime(NEUTRAL_TIME),
                DatePrecision.EXPLICIT_FULL,
                "data completa por extenso: $date"
            )
        }
        return null
    }

    // ---- "24/07", "24/07/2026", "24-07", "24 do 7" ---------------------------

    private val NUMERIC_DATE = Regex("""\b(\d{1,2})\s*(?:/|-|\s+do\s+)\s*(\d{1,2})(?:\s*[/-]\s*(\d{2,4}))?\b""")

    private fun resolveNumericDate(
        text: String,
        now: LocalDateTime,
        mask: ConsumptionMask
    ): Result? {
        for (match in NUMERIC_DATE.findAll(text)) {
            if (mask.overlaps(match.range)) continue
            val day = match.groupValues[1].toIntOrNull() ?: continue
            val month = match.groupValues[2].toIntOrNull() ?: continue
            if (month !in 1..12 || day !in 1..31) continue
            val rawYear = match.groupValues[3].toIntOrNull()
            val year = when {
                rawYear == null -> now.year
                rawYear < 100 -> 2000 + rawYear
                else -> rawYear
            }
            val date = safeDate(year, month, day) ?: continue
            mask.consume(match.range)
            return Result(
                date.atTime(NEUTRAL_TIME),
                DatePrecision.EXPLICIT_FULL,
                "data numérica: $date"
            )
        }
        return null
    }

    // ---- "dia 24" ------------------------------------------------------------

    private val EXPLICIT_DAY = Regex("""\bdia\s+(\d{1,2})\b""")

    private fun resolveExplicitDay(
        text: String,
        now: LocalDateTime,
        mask: ConsumptionMask,
        futureMarker: Boolean
    ): Result? {
        val match = EXPLICIT_DAY.find(text) ?: return null
        if (mask.overlaps(match.range)) return null
        val day = match.groupValues[1].toIntOrNull() ?: return null
        if (day !in 1..31) return null

        // Regra central: quem diz "dia 24" está registrando algo que já
        // aconteceu. Se o dia ainda não chegou neste mês, é do mês anterior.
        val base = if (day > now.dayOfMonth && !futureMarker) {
            now.toLocalDate().minusMonths(1)
        } else {
            now.toLocalDate()
        }

        val date = safeDate(base.year, base.monthValue, day) ?: return null
        mask.consume(match.range)

        val time = if (date == now.toLocalDate()) now.toLocalTime() else NEUTRAL_TIME
        return Result(
            date.atTime(time),
            DatePrecision.EXPLICIT_DAY,
            "dia explícito $day resolvido para $date"
        )
    }

    // ---- "há 3 dias", "3 dias atrás", "3 semanas atrás" ----------------------

    private val DAYS_AGO = Regex("""\b(?:ha\s+)?(\d{1,3})\s+(dias?|semanas?|mes(?:es)?)\s*(?:atras)?\b""")

    private fun resolveDaysAgo(
        text: String,
        now: LocalDateTime,
        mask: ConsumptionMask
    ): Result? {
        for (match in DAYS_AGO.findAll(text)) {
            if (mask.overlaps(match.range)) continue
            // Exige "ha" antes ou "atras" depois — senão "3 dias" pode ser outra coisa.
            val hasAnchor = match.value.startsWith("ha ") || match.value.contains("atras")
            if (!hasAnchor) continue

            val amount = match.groupValues[1].toLongOrNull() ?: continue
            val unit = match.groupValues[2]
            val date = when {
                unit.startsWith("dia") -> now.toLocalDate().minusDays(amount)
                unit.startsWith("semana") -> now.toLocalDate().minusWeeks(amount)
                else -> now.toLocalDate().minusMonths(amount)
            }
            mask.consume(match.range)
            return Result(
                date.atTime(NEUTRAL_TIME),
                DatePrecision.RELATIVE,
                "deslocamento relativo: $date"
            )
        }
        return null
    }

    // ---- "sexta passada", "na segunda", "próxima terça" ----------------------

    private val WEEKDAY_PATTERN = Regex(
        """\b(?:(proxim[ao])\s+)?(segunda|terca|quarta|quinta|sexta|sabado|domingo)(?:\s*-?\s*feira)?(?:\s+(passad[ao]|que\s+vem))?\b"""
    )

    private fun resolveWeekday(
        text: String,
        now: LocalDateTime,
        mask: ConsumptionMask,
        futureMarker: Boolean
    ): Result? {
        val match = WEEKDAY_PATTERN.find(text) ?: return null
        if (mask.overlaps(match.range)) return null
        val dow = WEEKDAYS[match.groupValues[2]] ?: return null

        val isFuture = match.groupValues[1].isNotEmpty() ||
            match.groupValues[3].contains("que") ||
            (futureMarker && match.groupValues[3].isEmpty())

        val date = if (isFuture) {
            now.toLocalDate().with(TemporalAdjusters.next(dow))
        } else {
            now.toLocalDate().with(TemporalAdjusters.previousOrSame(dow)).let {
                // "sexta passada" dito numa sexta significa a semana anterior.
                if (it == now.toLocalDate() && match.groupValues[3].startsWith("passad")) {
                    it.minusWeeks(1)
                } else it
            }
        }

        mask.consume(match.range)
        return Result(
            date.atTime(NEUTRAL_TIME),
            DatePrecision.RELATIVE,
            "dia da semana resolvido para $date"
        )
    }

    // ---- palavras-chave soltas ----------------------------------------------

    private val KEYWORDS = Regex(
        """\b(hoje|ontem|anteontem|amanha|depois de amanha|semana passada|mes passado|no inicio do mes|no fim do mes)\b"""
    )

    private fun resolveKeyword(
        text: String,
        now: LocalDateTime,
        mask: ConsumptionMask
    ): Result? {
        val match = KEYWORDS.find(text) ?: return null
        if (mask.overlaps(match.range)) return null
        val today = now.toLocalDate()

        val date = when (match.value) {
            "hoje" -> today
            "ontem" -> today.minusDays(1)
            "anteontem" -> today.minusDays(2)
            "amanha" -> today.plusDays(1)
            "depois de amanha" -> today.plusDays(2)
            "semana passada" -> today.minusWeeks(1)
            "mes passado" -> today.minusMonths(1)
            "no inicio do mes" -> today.withDayOfMonth(1)
            "no fim do mes" -> today.with(TemporalAdjusters.lastDayOfMonth())
            else -> return null
        }

        mask.consume(match.range)
        val time = if (date == today) now.toLocalTime() else NEUTRAL_TIME
        return Result(
            date.atTime(time),
            DatePrecision.RELATIVE,
            "palavra-chave '${match.value}' → $date"
        )
    }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate? = runCatching {
        val lengthOfMonth = LocalDate.of(year, month, 1).lengthOfMonth()
        LocalDate.of(year, month, day.coerceAtMost(lengthOfMonth))
    }.getOrNull()
}
