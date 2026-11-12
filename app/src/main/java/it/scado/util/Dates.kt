package it.scado.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object Dates {
    private val italian = Locale.ITALIAN
    private val formatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy", italian)

    fun today(): LocalDate = LocalDate.now()

    fun format(date: LocalDate): String =
        formatter.format(date).replaceFirstChar { it.titlecase(italian) }

    fun format(epochDay: Long): String = format(LocalDate.ofEpochDay(epochDay))

    fun daysLeft(epochDay: Long): Long = epochDay - today().toEpochDay()

    fun relative(epochDay: Long): String {
        val d = daysLeft(epochDay)
        return when {
            d < -1 -> "Scaduto da ${-d} giorni"
            d == -1L -> "Scaduto ieri"
            d == 0L -> "Scade oggi"
            d == 1L -> "Scade domani"
            else -> "Scade tra $d giorni"
        }
    }

    fun shortRelative(epochDay: Long): String {
        val d = daysLeft(epochDay)
        return when {
            d < 0 -> "scaduto"
            d == 0L -> "oggi"
            d == 1L -> "domani"
            else -> "$d gg"
        }
    }

    fun reminderLabel(days: Int): String = when (days) {
        0 -> "Il giorno stesso"
        1 -> "1 giorno prima"
        7 -> "1 settimana prima"
        14 -> "2 settimane prima"
        30 -> "1 mese prima"
        else -> "$days giorni prima"
    }

    /** Il DatePicker Material lavora in millisecondi UTC a mezzanotte. */
    fun utcMillisToLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    fun localDateToUtcMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
