package com.xapphire13.calendarwidget.components

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.layout.Row
import androidx.ui.layout.padding
import androidx.ui.material.Checkbox
import androidx.ui.material.ListItem
import androidx.ui.unit.dp

@Suppress("FunctionName")
@Composable
fun CheckboxGroup(
    options: List<String>,
    selectedOptions: Set<String>,
    onChange: (Set<String>) -> Unit
) {
    options.forEach {
        val handleCheckboxPressed = {
            val isCurrentlySelected = selectedOptions.contains(it)

            if (isCurrentlySelected) {
                onChange(selectedOptions.toMutableSet().apply { remove(it) })
            } else {
                onChange(selectedOptions.toMutableSet().apply { add(it) })
            }
        }

        ListItem(onClick = handleCheckboxPressed) {
            Row() {
                Checkbox(
                    checked = selectedOptions.contains(it),
                    onCheckedChange = { handleCheckboxPressed() })
                Text(text = it, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
