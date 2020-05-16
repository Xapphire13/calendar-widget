package com.xapphire13.calendarwidget

import java.time.Duration
import java.time.Instant

data class CalendarItem(val name: String, val start: Instant, val end: Instant) {
  fun isAllDay() = Duration.between(start, end).toDays() >= 1
}