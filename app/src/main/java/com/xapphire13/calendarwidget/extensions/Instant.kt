package com.xapphire13.calendarwidget.extensions

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Instant.toLocalDateTime(): LocalDateTime {
    return this.atZone(ZoneId.systemDefault()).toLocalDateTime()
}
