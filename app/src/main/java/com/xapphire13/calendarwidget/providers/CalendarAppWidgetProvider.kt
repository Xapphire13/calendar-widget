package com.xapphire13.calendarwidget.providers

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.xapphire13.calendarwidget.R
import com.xapphire13.calendarwidget.services.CalendarAppWidgetService

class CalendarAppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            performUpdate(context, appWidgetManager, appWidgetId)
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun performUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val rv = RemoteViews(
            context.packageName,
            R.layout.widget
        )

        Intent(context, CalendarAppWidgetService::class.java).apply {
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            rv.setRemoteAdapter(R.id.widget_list, this)
        }

        appWidgetManager.updateAppWidget(appWidgetId, rv)
        appWidgetManager.notifyAppWidgetViewDataChanged(
            appWidgetId,
            R.id.widget_list
        )
    }
}
