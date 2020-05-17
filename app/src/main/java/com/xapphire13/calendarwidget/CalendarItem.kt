package com.xapphire13.calendarwidget

import com.xapphire13.calendarwidget.extensions.toLocalDateTime
import java.time.Duration
import java.time.Instant

enum class CalendarItemStatus {
  ACCEPTED,
  PENDING
}

data class CalendarItem(
  val name: String,
  val start: Instant,
  val end: Instant,
  val status: CalendarItemStatus
) {
  fun isAllDay() = Duration.between(start, end).toDays() >= 1
  fun isMultiHour() = this.end.toLocalDateTime().hour - this.start.toLocalDateTime().hour > 1
}