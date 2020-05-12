package com.xapphire13.calendarwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

class CalendarAppWidgetProvider : AppWidgetProvider() {
  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    appWidgetIds.forEach { appWidgetId ->
      val intent = Intent(context, CalendarAppWidgetService::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
      }

      val rv = RemoteViews(context.packageName, R.layout.widget).apply {
        setRemoteAdapter(R.id.item_grid, intent)
      }

      appWidgetManager.updateAppWidget(appWidgetId, rv)
    }

    super.onUpdate(context, appWidgetManager, appWidgetIds)
  }
}