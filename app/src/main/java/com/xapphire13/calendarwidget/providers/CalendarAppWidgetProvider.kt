package com.xapphire13.calendarwidget.providers

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.widget.RemoteViews
import com.xapphire13.calendarwidget.R
import com.xapphire13.calendarwidget.services.CalendarAppWidgetService
import com.xapphire13.calendarwidget.utils.listEventsAsync
import kotlinx.coroutines.runBlocking
import java.time.temporal.ChronoUnit

class CalendarAppWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        appWidgetIds.forEach { appWidgetId ->
            performUpdate(context, appWidgetManager, appWidgetId)
        }

        scheduleNextUpdate(context, appWidgetManager)
    }

    private fun scheduleNextUpdate(context: Context, appWidgetManager: AppWidgetManager) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                context,
                CalendarAppWidgetProvider::class.java
            )
        )

        val intent = Intent(
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            null,
            context,
            CalendarAppWidgetProvider::class.java
        ).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime()
                .plus(ChronoUnit.MINUTES.duration.multipliedBy(2).toMillis()),
            pendingIntent
        )
    }

    private fun performUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val calendarId =
            context.getSharedPreferences("calendar", Context.MODE_PRIVATE).getLong("id", 0)

        val allCalendarItems = runBlocking {
            listEventsAsync(context.contentResolver, calendarId).await()
        }

        if (allCalendarItems.isEmpty()) {
            val rv = RemoteViews(context.packageName, R.layout.no_items)

            appWidgetManager.updateAppWidget(appWidgetId, rv)
        } else {
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
}
