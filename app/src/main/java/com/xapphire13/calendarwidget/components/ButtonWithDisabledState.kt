package com.xapphire13.calendarwidget.components

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.graphics.Color
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.material.contentColorFor
import androidx.ui.unit.dp

/**
 * Button that adds disabled state as per https://material.io/design/interaction/states.html#disabled
 */
@Composable
fun ButtonWithDisabledState(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  text: @Composable() () -> Unit
) {
  Button(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    backgroundColor = if (enabled) MaterialTheme.colors.primary else Color.LightGray,
    contentColor = if (enabled) contentColorFor(MaterialTheme.colors.primary) else Color.Gray,
    elevation = if (enabled) 2.dp else 0.dp
  ) {
    text()
  }
}