package com.xapphire13.calendarwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Model
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.Text
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.material.Divider
import androidx.ui.material.ListItem
import androidx.ui.material.MaterialTheme
import androidx.ui.unit.dp
import com.xapphire13.calendarwidget.utils.listCalendarsAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

@Model
data class ConfigureWidgetState(var calendars: List<CalendarInfo> = listOf())

val uiScope = CoroutineScope(Dispatchers.Main)

class ConfigureWidgetActivity : AppCompatActivity() {
  val state = ConfigureWidgetState()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setResult(Activity.RESULT_CANCELED)

    loadCalendarsAsync()

    setContent {
      MaterialTheme {
        Column(
          modifier = Modifier.fillMaxWidth().plus(Modifier.padding(16.dp))
        ) {
          Text(text = "Choose which calendars to use")

          state.calendars.forEachIndexed { i, calendar ->
            if (i != 0) {
              Divider(color = Color.LightGray)
            }

            ListItem(
              onClick = { onCalendarClicked(calendar.id) }
            ) {
              Text(text = calendar.name)
            }
          }
        }
      }
    }
  }

  private fun loadCalendarsAsync() = uiScope.async {
    state.calendars = listCalendarsAsync(contentResolver).await()
  }

  private fun onCalendarClicked(calendarId: Long) {
    getSharedPreferences("calendar", Context.MODE_PRIVATE).edit().putLong("id", calendarId).apply()

    val appWidgetManager = AppWidgetManager.getInstance(this)
    val appWidgetId = intent?.extras?.getInt(
      AppWidgetManager.EXTRA_APPWIDGET_ID,
      AppWidgetManager.INVALID_APPWIDGET_ID
    ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      finish()
      return
    }

    setResult(Activity.RESULT_OK)

    CalendarAppWidgetProvider().onUpdate(this, appWidgetManager, intArrayOf(appWidgetId))
    finish()
  }
}