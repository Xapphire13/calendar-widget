package com.xapphire13.calendarwidget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.LocalDateTime

class CalendarAppWidgetService : RemoteViewsService() {
  override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
    CalendarAppWidgetFactory(this.applicationContext)
}

class CalendarAppWidgetFactory(
  private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
  private val calendarItems: List<CalendarItem> = listOf(
    CalendarItem(
      "Test all day event",
      LocalDateTime.now().minusDays(1),
      LocalDateTime.now()
    ),
    CalendarItem(
      "Another event",
      LocalDateTime.now().minusDays(1),
      LocalDateTime.now()
    ),
    CalendarItem(
      "My 10 o' clock",
      LocalDateTime.of(2020, 1, 1, 10, 0),
      LocalDateTime.of(2020, 1, 1, 11, 0)
    ),
    CalendarItem(
      "☕️ break",
      LocalDateTime.of(2020, 1, 1, 10, 34),
      LocalDateTime.of(2020, 1, 1, 12, 0)
    ),
    CalendarItem(
      "1:1 w/Tim",
      LocalDateTime.of(2020, 1, 1, 15, 0),
      LocalDateTime.of(2020, 1, 1, 15, 30)
    )
  )

  private val items: MutableList<Pair<String, CalendarItem?>> = mutableListOf()

  override fun onCreate() {
    val (allDayItems, otherItems) = calendarItems.partition { it.isAllDay() }
    val itemsByTime =
      otherItems.groupBy({ it.start.hour }, { it })

    allDayItems.forEachIndexed { i, item ->
      val key = if (i == 0) "all-day" else ""

      items.add(Pair(key, item))
    }

    if (otherItems.isEmpty()) {
      return
    }

    val firstHour = otherItems.first().start.hour
    val lastHour = otherItems.last().start.hour

    (0 until 24).forEach { hour ->
      if (hour < firstHour || hour > lastHour) {
        return@forEach
      }

      val key =
        if (hour == 0) "midnight" else if (hour < 12) "$hour AM" else if (hour == 12) "12 PM" else "${hour - 12} PM"

      itemsByTime[hour]?.sortedBy { it.start }?.forEachIndexed { i, item ->
        items.add(Pair(if (i == 0) key else "", item))
      } ?: run {
        items.add(Pair(key, null))
      }
    }
  }

  override fun getLoadingView(): RemoteViews? = null

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun onDataSetChanged() {}

  override fun hasStableIds(): Boolean = true

  override fun getViewAt(position: Int): RemoteViews {
    val (key, item) = items[position]

    val rv = RemoteViews(context.packageName, R.layout.calendar_row)

    val label = RemoteViews(context.packageName, R.layout.time_label).apply {
      setTextViewText(R.id.time_label_text, key)
    }

    val calendarItem = RemoteViews(
      context.packageName,
      if (item?.isAllDay() == true) R.layout.all_day_calendar_item else R.layout.calendar_item
    ).apply {
      setTextViewText(R.id.calendar_item_text, item?.name)
    }

    rv.apply {
      addView(R.id.calendar_row_root, label)
      addView(R.id.calendar_row_root, calendarItem)
    }

    return rv
  }

  override fun getCount(): Int {
    return items.size
  }

  override fun getViewTypeCount(): Int = 1

  override fun onDestroy() {}

}