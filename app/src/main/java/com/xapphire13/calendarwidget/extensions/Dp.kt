package com.xapphire13.calendarwidget.extensions

import android.content.Context
import android.util.DisplayMetrics
import androidx.ui.unit.Dp
import kotlin.math.roundToInt

// Adapted from https://stackoverflow.com/a/9563438
fun Dp.toPixels(context: Context): Int {
  return (this.value * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
}