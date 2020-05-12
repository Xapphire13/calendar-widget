package com.xapphire13.calendarwidget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class CalendarAppWidgetService : RemoteViewsService() {
  override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
    CalendarAppWidgetFactory(this.applicationContext, intent)
}

class CalendarAppWidgetFactory(
  private val context: Context,
  intent: Intent
) : RemoteViewsService.RemoteViewsFactory {
  companion object {
    private val NUMBER_OF_ITEMS = 50
  }

  private val items: MutableList<String> = emptyList<String>().toMutableList()

  override fun onCreate() {
    (0 until NUMBER_OF_ITEMS).forEach {
      items.add("$it")
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

    // Label
    val rv = if (col == 0) {
      RemoteViews(context.packageName, R.layout.time_label).apply {
        if (row == 0) {
          setTextViewText(R.id.time_label_text, "all-day")
        } else {
          val hour = (row - 1) / 2
          val minute = if (row - 1 % 2 == 0) "00" else "30"
          setTextViewText(R.id.time_label_text, "$hour:$minute")
        }
      }
    } else {
      RemoteViews(context.packageName, R.layout.calendar_item).apply {
        setTextViewText(R.id.calendar_item_text, "nothing")
      }
    }

    return rv
  }

  override fun getCount(): Int {
    return items.size
  }

  override fun getViewTypeCount(): Int = 2

  override fun onDestroy() {}

}