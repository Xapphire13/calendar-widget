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
  private var allDayItems: List<CalendarItem> = listOf()
  private val itemColumn: MutableMap<CalendarItem, Int> = mutableMapOf()
  private val itemOverlap: MutableMap<CalendarItem, Int> = mutableMapOf()
  private var itemsByTime: MutableMap<Int, MutableList<CalendarItem>> = mutableMapOf()

  override fun onCreate() {
    val calendarId = context.getSharedPreferences("calendar", Context.MODE_PRIVATE).getLong("id", 0)

    val allCalendarItems = runBlocking {
      listEventsAsync(context.contentResolver, calendarId).await()
    }

    val (allDayItems, otherItems) = allCalendarItems.sortedBy { it.start }
      .partition { it.isAllDay() }
    this.allDayItems = allDayItems
    otherItems.forEach {
      val startHour = it.start.toLocalDateTime().hour
      val endHour =
        it.end.toLocalDateTime().hour.let { endHour -> if (it.end.toLocalDateTime().minute > 0) endHour + 1 else endHour }

      (startHour until endHour).forEach { hour ->
        val items = itemsByTime.getOrPut(hour, { mutableListOf() })
        items.add(it)
      }
    }

    if (itemsByTime.isEmpty()) {
      return
    }

    // Calculate overlaps
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

    // Calculate columns for each item
    (0 until 24).forEach { hour ->
      val calendarItems = itemsByTime[hour]

      if (calendarItems === null || calendarItems.isEmpty()) {
        return@forEach
      }

      val maxOverlap = calendarItems.fold(0) { acc, item ->
        maxOf(acc, itemOverlap.getOrDefault(item, 0))
      }

      val cells = (0 until maxOverlap).map { false }.toBooleanArray()

      // Block out spaces that are already claimed
      calendarItems.filter { itemColumn.containsKey(it) }.forEach { item ->
        val column =
          itemColumn[item] ?: throw RuntimeException("Can't get column for item ${item.name}")

        cells[column] = true
      }

      // Find columns for unplaced items
      calendarItems.filter { !itemColumn.containsKey(it) }.forEach { item ->
        val column = cells.indexOfFirst { !it }

        if (column == -1) {
          throw RuntimeException("No space to add item ${item.name}")
        }

        cells[column] = true
        itemColumn[item] = column
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
    val (hour, calendarItems) = itemsByTime.entries.sortedBy { it.key }[position]
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

    // Place items into their column
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
    return allDayItems.size + itemsByTime.entries.size
  }

  override fun getViewTypeCount(): Int = 1

  override fun onDestroy() {}

}