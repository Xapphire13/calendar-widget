package com.xapphire13.calendarwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.Text
import androidx.ui.layout.Column
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.unit.dp

class ConfigureWidgetActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setResult(Activity.RESULT_CANCELED)

    setContent {
      MaterialTheme {
        Column(
          modifier = Modifier.fillMaxWidth().plus(Modifier.padding(16.dp))
        ) {
          Text(text = "Choose which calendars to use")

          Button(
            modifier = Modifier.gravity(Alignment.CenterHorizontally),
            onClick = this@ConfigureWidgetActivity::onClick
          ) {
            Text(text = "Click")
          }
        }
      }
    }
  }

  private fun onClick() {
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