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
  private val items: MutableList<Pair<String, List<CalendarItem>>> = mutableListOf()

  override fun onCreate() {
    val calendarId = context.getSharedPreferences("calendar", Context.MODE_PRIVATE).getLong("id", 0)

    val calendarItems = runBlocking {
      listEventsAsync(context.contentResolver, calendarId).await()
    }

    val (allDayItems, otherItems) = calendarItems.sortedBy { it.start }.partition { it.isAllDay() }
    val itemsByTime = mutableMapOf<Int, MutableList<CalendarItem>>()
    otherItems.forEach {
      val startHour = it.start.toLocalDateTime().hour
      val endHour =
        it.end.toLocalDateTime().hour.let { endHour -> if (it.end.toLocalDateTime().minute > 0) endHour + 1 else endHour }

      (startHour until endHour).forEach { hour ->
        val items = itemsByTime.getOrPut(hour, { mutableListOf() })
        items.add(it)
      }
    }

    if (allDayItems.isNotEmpty()) {
      items.add(Pair("all-day", allDayItems))
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

      itemsByTime[hour]?.let {
        items.add(Pair(key, it))
      } ?: run {
        items.add(Pair(key, listOf()))
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
    val (key, items) = items[position]

    println("$position $key $items")

    // TODO, all day row

    val rv = RemoteViews(context.packageName, R.layout.calendar_row)

    val label = RemoteViews(context.packageName, R.layout.time_label).apply {
      setTextViewText(R.id.time_label_text, key)
    }

    val calendarItems = items.map { item ->
      RemoteViews(context.packageName, R.layout.calendar_item).apply {
        setTextViewText(R.id.calendar_item_text, item.name)
      }
    }

    rv.apply {
      addView(R.id.calendar_row_root, label)
      calendarItems.forEach {
        addView(R.id.calendar_row_root, it)
      }
    }

    return rv
  }

  override fun getCount(): Int {
    return items.size
  }

  override fun getViewTypeCount(): Int = 1

  override fun onDestroy() {}

}