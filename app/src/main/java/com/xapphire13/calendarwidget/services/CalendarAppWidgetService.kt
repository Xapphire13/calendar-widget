package com.xapphire13.calendarwidget.services

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.ui.unit.dp
import com.xapphire13.calendarwidget.R
import com.xapphire13.calendarwidget.extensions.toLocalDateTime
import com.xapphire13.calendarwidget.extensions.toPixels
import com.xapphire13.calendarwidget.models.CalendarItem
import com.xapphire13.calendarwidget.models.CalendarItemStatus
import com.xapphire13.calendarwidget.utils.listEventsAsync
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import kotlin.math.roundToInt

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

    override fun onCreate() = Unit

    override fun getLoadingView(): RemoteViews? = null

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onDataSetChanged() {
        val calendarId =
            context.getSharedPreferences("calendar", Context.MODE_PRIVATE).getLong("id", 0)

        val allCalendarItems = runBlocking {
            listEventsAsync(context.contentResolver, calendarId).await()
        }

        val (allDayItems, otherItems) = allCalendarItems.sortedBy { it.start }
            .partition { it.isAllDay() }
        this.allDayItems = allDayItems
        otherItems.forEach {
            val startHour = it.start.toLocalDateTime().hour
            val endHour =
                it.end.toLocalDateTime().hour.let { endHour ->
                    if (it.end.toLocalDateTime().minute > 0) endHour + 1 else endHour
                }

            (startHour until endHour).forEach { hour ->
                val items = itemsByTime.getOrPut(hour, { mutableListOf() })
                items.add(it)
            }
        }

        if (itemsByTime.isEmpty()) {
            return
        }

        // Fill in empty between hours
        val minHour = itemsByTime.keys.min() ?: 0
        val maxHour = itemsByTime.keys.max() ?: 0
        for (hour in minHour until maxHour) {
            itemsByTime.putIfAbsent(hour, mutableListOf())
        }

        // Calculate overlaps
        for (hour in 0 until 24) {
            val itemsForHour = itemsByTime[hour]

            if (itemsForHour == null || itemsForHour.isEmpty()) {
                continue
            }

            itemsForHour.forEach { item ->
                val overlap = itemOverlap.getOrPut(item, { 0 })
                itemOverlap[item] = maxOf(overlap, itemsForHour.size)
            }
        }

        // Calculate columns for each item
        for (hour in 0 until 24) {
            val calendarItems = itemsByTime[hour]

            if (calendarItems === null || calendarItems.isEmpty()) {
                continue
            }

            val maxOverlap = calendarItems.fold(0) { acc, item ->
                maxOf(acc, itemOverlap.getOrDefault(item, 0))
            }

            val cells = (0 until maxOverlap).map { false }.toBooleanArray()

            // Block out spaces that are already claimed
            calendarItems.filter { itemColumn.containsKey(it) }.forEach { item ->
                val column =
                    itemColumn[item]
                        ?: throw RuntimeException("Can't get column for item ${item.name}")

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

    override fun hasStableIds(): Boolean = false

    override fun getViewAt(position: Int): RemoteViews {
        if (position == 0 && allDayItems.isNotEmpty()) {
            return RemoteViews(
                context.packageName,
                R.layout.all_day_items_row
            ).apply {
                removeAllViews(R.id.all_day_items_container)
                allDayItems.map { createAllDayItemView(it) }.forEach {
                    addView(R.id.all_day_items_container, it)
                }
            }
        }

        val index = if (allDayItems.isEmpty()) position else position - 1
        val (hour, calendarItems) = itemsByTime.entries.sortedBy { it.key }[index]

        return RemoteViews(
            context.packageName,
            R.layout.calendar_row
        ).apply {
            removeAllViews(R.id.calendar_row_root)
            addView(R.id.calendar_row_root, createLabelView(hour))
            createCalendarItemViews(hour, calendarItems).forEach {
                addView(R.id.calendar_row_root, it)
            }

            val currentTime = LocalTime.now()

            if (currentTime.hour == hour) {
                val percentageOfHour = currentTime.minute.toFloat() / 60

                setViewVisibility(R.id.hour_rule, View.VISIBLE)
                setViewPadding(
                    R.id.hour_rule,
                    0,
                    (percentageOfHour * 40).dp.toPixels(context).roundToInt(),
                    0,
                    0
                )
            } else {
                setViewVisibility(R.id.hour_rule, View.GONE)
            }
        }
    }

    override fun getCount(): Int {
        return (if (allDayItems.isEmpty()) 0 else 1) + itemsByTime.entries.size
    }

    override fun getViewTypeCount(): Int = 2

    override fun onDestroy() = Unit

    private fun createLabelView(hour: Int): RemoteViews {
        val labelText = when {
            hour == -1 -> "all-day"
            hour == 0 -> "midnight"
            hour < 12 -> "$hour AM"
            hour == 12 -> "12 PM"
            else -> "${hour - 12} PM"
        }

        return RemoteViews(
            context.packageName,
            R.layout.time_label
        ).apply {
            setTextViewText(R.id.time_label_text, labelText)
        }
    }

    private fun createCalendarItemViews(
        hour: Int,
        calendarItems: List<CalendarItem>
    ): List<RemoteViews> {
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
                (item.end.toLocalDateTime().hour.let { endHour ->
                    if (item.end.toLocalDateTime().minute > 0) endHour + 1 else endHour
                }) - 1 == hour
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

                if (item.status === CalendarItemStatus.ACCEPTED) {
                    setViewVisibility(R.id.accepted_background, View.VISIBLE)
                } else {
                    setViewVisibility(R.id.pending_background, View.VISIBLE)
                }
            }
        }

        // Fill the blanks
        cells.forEachIndexed { i, _ ->
            if (cells[i] == null) {
                cells[i] = RemoteViews(
                    context.packageName,
                    R.layout.blank_space
                )
            }
        }

        return cells.filterNotNull()
    }

    private fun createAllDayItemView(item: CalendarItem): RemoteViews {
        return RemoteViews(
            context.packageName,
            R.layout.all_day_calendar_item
        ).apply {
            setTextViewText(R.id.calendar_item_text, item.name)

            if (item.status === CalendarItemStatus.ACCEPTED) {
                setViewVisibility(R.id.all_day_accepted_background, View.VISIBLE)
            } else {
                setViewVisibility(R.id.all_day_pending_background, View.VISIBLE)
            }
        }
    }
}
