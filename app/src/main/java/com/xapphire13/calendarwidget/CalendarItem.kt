package com.xapphire13.calendarwidget

import java.time.Duration
import java.time.LocalDateTime

data class CalendarItem(val name: String, val start: LocalDateTime, val end: LocalDateTime) {
  fun isAllDay() = Duration.between(start, end).toDays() >= 1
}