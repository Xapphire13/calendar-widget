package com.xapphire13.calendarwidget.extensions

import android.content.Context
import android.util.DisplayMetrics
import androidx.ui.unit.Dp

// Modified from https://stackoverflow.com/a/9563438
fun Dp.toPixels(context: Context): Float {
    return this.value * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}
