package com.xapphire13.calendarwidget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CalendarAppWidgetService : RemoteViewsService() {
  override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
    CalendarAppWidgetFactory(this.applicationContext, intent)
}

class CalendarAppWidgetFactory(
  private val context: Context,
  intent: Intent
) : RemoteViewsService.RemoteViewsFactory {
  private val calendarItems: List<CalendarItem> = listOf(
    CalendarItem(
      "Test all day event",
      LocalDateTime.now().minusDays(1),
      LocalDateTime.now()
    ),
    CalendarItem(
      "My 10 o' clock",
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
      otherItems.associateBy({ it.start.hour }, { it })

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

      val item = itemsByTime.get(hour)
      var key = if (item != null) {
        item.start.format(DateTimeFormatter.ofPattern("h a"))
      } else {
        if (hour == 0) "midnight" else if (hour < 12) "$hour AM" else if (hour == 12) "12 PM" else "${hour - 12} PM"
      }

      items.add(Pair(key, item))
    }
  }

  override fun getLoadingView(): RemoteViews? = null

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun onDataSetChanged() {}

  override fun hasStableIds(): Boolean = true

  override fun getViewAt(position: Int): RemoteViews {
    val col = position % 2;
    val row = position / 2;
    val (key, item) = items[row]

    println(position)
    println(key)
    println(item)

    val rv = if (col == 0) {
      // Label
      RemoteViews(context.packageName, R.layout.time_label).apply {
        setTextViewText(R.id.time_label_text, key)
      }
    } else {
      // Item
      RemoteViews(context.packageName, R.layout.calendar_item).apply {
        setTextViewText(R.id.calendar_item_text, item?.name)
      }
    }

    return rv
  }

  override fun getCount(): Int {
    return 2 * items.size
  }

  override fun getViewTypeCount(): Int = 2

  override fun onDestroy() {}

}