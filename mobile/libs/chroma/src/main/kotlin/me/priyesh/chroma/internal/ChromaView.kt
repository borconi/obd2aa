/*
 * Copyright 2016 Priyesh Patel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.priyesh.chroma.internal

import android.content.Context
import android.graphics.Color
import android.support.annotation.ColorInt
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import me.priyesh.chroma.ColorMode
import me.priyesh.chroma.R

internal class ChromaView : RelativeLayout {

  companion object {
    val DefaultColor = Color.GRAY
    val DefaultModel = ColorMode.RGB
  }

  @ColorInt var currentColor: Int private set

  val colorMode: ColorMode

  constructor(context: Context) : this(DefaultColor, DefaultModel, context)

  constructor(@ColorInt initialColor: Int, colorMode: ColorMode, context: Context) : super(context) {
    this.currentColor = initialColor
    this.colorMode = colorMode
    init()
  }

  private fun init(): Unit {
    inflate(context, R.layout.chroma_view, this)
    clipToPadding = false

    val colorView = findViewById(R.id.color_view)
    colorView.setBackgroundColor(currentColor)

    val channelViews = colorMode.channels.map { ChannelView(it, currentColor, context) }

    val seekbarChangeListener: () -> Unit = {
      currentColor = colorMode.evaluateColor(channelViews.map { it.channel })
      colorView.setBackgroundColor(currentColor)
    }

    val channelContainer = findViewById(R.id.channel_container) as ViewGroup
    channelViews.forEach { it ->
      channelContainer.addView(it)

      val layoutParams = it.layoutParams as LinearLayout.LayoutParams
      layoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.channel_view_margin_top)
      layoutParams.bottomMargin = resources.getDimensionPixelSize(R.dimen.channel_view_margin_bottom)

      it.registerListener(seekbarChangeListener)
    }
  }

  internal interface ButtonBarListener {
    fun onPositiveButtonClick(color: Int)
    fun onNegativeButtonClick()
  }

  internal fun enableButtonBar(listener: ButtonBarListener?): Unit {
    with(findViewById(R.id.button_bar)) {
      val positiveButton = findViewById(R.id.positive_button)
      val negativeButton = findViewById(R.id.negative_button)

      if (listener != null) {
        visibility = VISIBLE
        positiveButton.setOnClickListener { listener.onPositiveButtonClick(currentColor) }
        negativeButton.setOnClickListener { listener.onNegativeButtonClick() }
      } else {
        visibility = GONE
        positiveButton.setOnClickListener(null)
        negativeButton.setOnClickListener(null)
      }
    }
  }
}