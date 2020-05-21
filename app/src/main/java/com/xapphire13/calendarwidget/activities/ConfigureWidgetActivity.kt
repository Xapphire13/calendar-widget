package com.xapphire13.calendarwidget.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Model
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.foundation.Text
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.material.MaterialTheme
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.unit.dp
import com.xapphire13.calendarwidget.components.ButtonWithDisabledState
import com.xapphire13.calendarwidget.components.CheckboxGroup
import com.xapphire13.calendarwidget.models.CalendarInfo
import com.xapphire13.calendarwidget.providers.CalendarAppWidgetProvider
import com.xapphire13.calendarwidget.utils.listCalendarsAsync
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

@Model
data class ConfigureWidgetState(
    var calendars: List<CalendarInfo> = listOf(),
    var selectedCalendars: Set<String> = setOf()
)

val uiScope = CoroutineScope(Dispatchers.Main)

class ConfigureWidgetActivity : AppCompatActivity() {
    private val state = ConfigureWidgetState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        loadCalendarsAsync()

        setContent {
            val isButtonEnabled = state.selectedCalendars.isNotEmpty()

            MaterialTheme {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(16.dp)
                ) {
                    Column() {
                        Text(
                            text = "Choose which calendars to use",
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        CheckboxGroup(
                            options = state.calendars.map { it.name },
                            selectedOptions = state.selectedCalendars,
                            onChange = { state.selectedCalendars = it })
                    }

                    ButtonWithDisabledState(
                        onClick = {
                            val calendar =
                                state.calendars.find { it.name == state.selectedCalendars.first() }
                            calendar?.let {
                                onCalendarClicked(it.id)
                            }
                        },
                        enabled = isButtonEnabled,
                        modifier = Modifier.gravity(Alignment.CenterHorizontally).fillMaxWidth()
                    ) {
                        Text(
                            text = "Select".toUpperCase(Locale.getDefault()),
                            style = TextStyle(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    private fun loadCalendarsAsync() = uiScope.async {
        state.calendars = listCalendarsAsync(contentResolver).await()
    }

    private fun onCalendarClicked(calendarId: Long) {
        getSharedPreferences("calendar", Context.MODE_PRIVATE).edit().putLong("id", calendarId)
            .apply()

        val appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setResult(Activity.RESULT_OK)

        Intent(
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            null,
            this,
            CalendarAppWidgetProvider::class.java
        ).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            sendBroadcast(this)
        }

        finish()
    }
}
