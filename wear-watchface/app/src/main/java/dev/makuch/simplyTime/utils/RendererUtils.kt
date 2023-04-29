package dev.makuch.simplyTime.utils

import java.time.DayOfWeek

fun mapDayOfWeek(dayOfWeek: DayOfWeek) = when(dayOfWeek) {
    DayOfWeek.MONDAY -> "pon"
    DayOfWeek.TUESDAY -> "wt"
    DayOfWeek.WEDNESDAY -> "śr"
    DayOfWeek.THURSDAY -> "czw"
    DayOfWeek.FRIDAY -> "pt"
    DayOfWeek.SATURDAY -> "sob"
    DayOfWeek.SUNDAY -> "nd"
    else -> "pon"
}
