package me.priyesh.chroma

import android.content.Context
import android.graphics.Color
import android.util.DisplayMetrics
import android.view.WindowManager

internal fun screenDimensions(context: Context): DisplayMetrics {
  val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  val metrics = DisplayMetrics()
  manager.defaultDisplay.getMetrics(metrics)
  return metrics
}

internal fun orientation(context: Context) = context.resources.configuration.orientation

internal infix fun Int.percentOf(n: Int): Int = (n * (this / 100.0)).toInt()

fun hue(color: Int): Int = hsv(color, 0)
fun saturation(color: Int): Int = hsv(color, 1, 100)
fun value(color: Int): Int = hsv(color, 2, 100)

private fun hsv(color: Int, index: Int, multiplier: Int = 1): Int {
  val hsv = FloatArray(3)
  Color.colorToHSV(color, hsv)
  return (hsv[index] * multiplier).toInt()
}