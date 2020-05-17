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
  private val items: MutableList<Pair<Int, List<CalendarItem>>> = mutableListOf()
  private val itemOverlap: MutableMap<CalendarItem, Int> = mutableMapOf()
  private val itemColumn: MutableMap<CalendarItem, Int> = mutableMapOf()

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
      items.add(Pair(-1, allDayItems))
    }

    if (otherItems.isEmpty()) {
      return
    }

    val firstHour =
      otherItems.fold(Int.MAX_VALUE) { acc, item -> minOf(acc, item.start.toLocalDateTime().hour) }
    val lastHour =
      otherItems.fold(Int.MIN_VALUE) { acc, item -> maxOf(acc, item.end.toLocalDateTime().hour) }

    (0 until 24).forEach { hour ->
      if (hour < firstHour || hour > lastHour) {
        return@forEach
      }

      itemsByTime[hour]?.let {
        items.add(Pair(hour, it))
      }
    }

    (0 until 24).forEach { hour ->
      val itemsForHour = itemsByTime[hour];

      if (itemsForHour == null || itemsForHour.isEmpty()) {
        return@forEach
      }

      itemsForHour.forEach { item ->
        val overlap = itemOverlap.getOrPut(item, { 0 })
        itemOverlap[item] = maxOf(overlap, itemsForHour.size)
      }
    }

    itemsByTime.entries.forEach {
      println(it)
    }
  }

  override fun getLoadingView(): RemoteViews? = null

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun onDataSetChanged() {}

  override fun hasStableIds(): Boolean = false

  override fun getViewAt(position: Int): RemoteViews {
    val (hour, calendarItems) = items[position]
    val labelText = when {
      hour == -1 -> "all-day"
      hour == 0 -> "midnight"
      hour < 12 -> "$hour AM"
      hour == 12 -> "12 PM"
      else -> "${hour - 12} PM"
    }

    // TODO, all day row

    val rv = RemoteViews(context.packageName, R.layout.calendar_row)

    val labelView = RemoteViews(context.packageName, R.layout.time_label).apply {
      setTextViewText(R.id.time_label_text, labelText)
    }

    val maxOverlap = calendarItems.fold(0) { acc, item ->
      maxOf(acc, itemOverlap.getOrDefault(item, 0))
    }

    val cells = arrayOfNulls<RemoteViews>(maxOverlap)

    // Place items that have been placed before
    calendarItems.filter { itemColumn.containsKey(it) }.forEach { item ->
      val column =
        itemColumn[item] ?: throw RuntimeException("Can't get column for item ${item.name}")
      val isStartOfItem = item.start.toLocalDateTime().hour == hour
      val isEndOfItem =
        (item.end.toLocalDateTime().hour.let { endHour -> if (item.end.toLocalDateTime().minute > 0) endHour + 1 else endHour }) - 1 == hour
      val layout = when {
        !item.isMultiHour() -> R.layout.calendar_item_full
        isStartOfItem -> R.layout.calendar_item_top
        isEndOfItem -> R.layout.calendar_item_bottom
        else -> R.layout.calendar_item_mid
      }

      cells[column] = RemoteViews(context.packageName, layout).apply {
        if (isStartOfItem) {
          setTextViewText(R.id.calendar_item_text, item.name)
        }
      }
    }

    // Place items that are being placed for the first time
    calendarItems.filter { !itemColumn.containsKey(it) }.forEach { item ->
      val column = cells.indexOfFirst { it == null }

      if (column == -1) {
        throw RuntimeException("No space to add item ${item.name}")
      }

      val isStartOfItem = item.start.toLocalDateTime().hour == hour
      val layout = when {
        !item.isMultiHour() -> R.layout.calendar_item_full
        else -> R.layout.calendar_item_top
      }

      cells[column] = RemoteViews(context.packageName, layout).apply {
        if (isStartOfItem) {
          setTextViewText(R.id.calendar_item_text, item.name)
        }
      }

      itemColumn[item] = column
    }

    // Fill the blanks
    cells.forEachIndexed { i, _ ->
      if (cells[i] == null) {
        cells[i] = RemoteViews(context.packageName, R.layout.blank_space)
      }
    }

    rv.apply {
      addView(R.id.calendar_row_root, labelView)
      cells.forEach {
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