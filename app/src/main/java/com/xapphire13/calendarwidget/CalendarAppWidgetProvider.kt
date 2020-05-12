package com.xapphire13.calendarwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import java.util.stream.IntStream

class CalendarAppWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    super.onUpdate(context, appWidgetManager, appWidgetIds)

    appWidgetIds.forEach {
      val rv = RemoteViews(context.packageName, R.layout.widget)
      rv.removeAllViews(R.id.item_container)

      IntStream.range(0, 5).forEach { i ->
        val item = RemoteViews(context.packageName, R.layout.calendar_item)
        item.setTextViewText(R.id.calendar_item_text, "Test" + (i + 1))

        rv.addView(R.id.item_container, item);
      }

      appWidgetManager.updateAppWidget(it, rv)
    }
  }
}