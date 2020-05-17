package com.xapphire13.calendarwidget.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.provider.CalendarContract
import com.xapphire13.calendarwidget.CalendarInfo
import com.xapphire13.calendarwidget.CalendarItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

val bgScope = CoroutineScope(Dispatchers.IO)

@SuppressLint("MissingPermission")
fun listCalendarsAsync(contentResolver: ContentResolver): Deferred<List<CalendarInfo>> =
  bgScope.async {
    val calendars = mutableListOf<CalendarInfo>()

    contentResolver.query(
      CalendarContract.Calendars.CONTENT_URI,
      arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        CalendarContract.Calendars.ACCOUNT_NAME
      ),
      null,
      null,
      null
    ).use { cursor ->
      while (cursor?.moveToNext() == true) {
        val calID = cursor.getLong(0)
        val calName = cursor.getString(1)
        val accountName = cursor.getString(2)

        calendars.add(CalendarInfo(calID, calName, accountName))
      }
    }

    calendars
  }

@SuppressLint("MissingPermission")
fun listEventsAsync(
  contentResolver: ContentResolver,
  calendarID: Long
): Deferred<List<CalendarItem>> =
  bgScope.async {
    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    val items = mutableListOf<CalendarItem>()

    ContentUris.appendId(
      builder,
      LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    ContentUris.appendId(builder, Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli())

    contentResolver.query(
      builder.build(),
      arrayOf(
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.DTSTART,
        CalendarContract.Instances.DTEND
      ),
      """
        ((${CalendarContract.Instances.CALENDAR_ID} = ?))
      """.trimIndent(),
      arrayOf(
        calendarID.toString()
      ),
      null
    ).use { cursor ->
      while (cursor?.moveToNext() == true) {
        val title = cursor.getString(0)
        val dtStart = cursor.getLong(1)
        val dtEnd = cursor.getLong(2)

        items.add(CalendarItem(title, Instant.ofEpochMilli(dtStart), Instant.ofEpochMilli(dtEnd)))
      }
    }

    items
  }
