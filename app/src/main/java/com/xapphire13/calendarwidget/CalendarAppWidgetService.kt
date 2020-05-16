package com.xapphire13.calendarwidget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.xapphire13.calendarwidget.extensions.toLocalDateTime
import com.xapphire13.calendarwidget.utils.listEventsAsync
import kotlinx.coroutines.runBlocking

class CalendarAppWidgetService : RemoteViewsService() {
  override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
    CalendarAppWidgetFactory(this.applicationContext)
}

class CalendarAppWidgetFactory(
  private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
  private val items: MutableList<Pair<String, CalendarItem?>> = mutableListOf()

  override fun onCreate() {
    val calendarId = context.getSharedPreferences("calendar", Context.MODE_PRIVATE).getLong("id", 0)

    val calendarItems = runBlocking {
      listEventsAsync(context.contentResolver, calendarId).await()
    }

    val (allDayItems, otherItems) = calendarItems.partition { it.isAllDay() }
    val itemsByTime =
      otherItems.groupBy({ it.start.toLocalDateTime().hour }, { it })

    allDayItems.forEachIndexed { i, item ->
      val key = if (i == 0) "all-day" else ""

      items.add(Pair(key, item))
    }

    if (otherItems.isEmpty()) {
      return
    }

    val firstHour = otherItems.first().start.toLocalDateTime().hour
    val lastHour = otherItems.last().start.toLocalDateTime().hour

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

  override fun hasStableIds(): Boolean = false

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