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
import android.support.annotation.ColorInt
import android.view.View
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import me.priyesh.chroma.R
import me.priyesh.chroma.ColorMode

internal class ChannelView(
    val channel: ColorMode.Channel,
    @ColorInt color: Int,
    context: Context) : RelativeLayout(context) {

  internal var listener: (() -> Unit)? = null

  init {
    channel.progress = channel.extractor.invoke(color)

    if (channel.progress < channel.min || channel.progress > channel.max) {
      throw IllegalArgumentException("Initial progress for channel: ${channel.javaClass.simpleName}"
          + " must be between ${channel.min} and ${channel.max}.")
    }

    val rootView = inflate(context, R.layout.channel_row, this)
    bindViews(rootView)
  }

  private fun bindViews(root: View): Unit {
    (root.findViewById(R.id.label) as TextView).text = context.getString(channel.nameResourceId)

    val progressView = root.findViewById(R.id.progress_text) as TextView
    progressView.text = channel.progress.toString()

    val seekbar = root.findViewById(R.id.seekbar) as SeekBar
    seekbar.max = channel.max
    seekbar.progress = channel.progress
    seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onStartTrackingTouch(seekbar: SeekBar?) { }

      override fun onStopTrackingTouch(seekbar: SeekBar?) { }

      override fun onProgressChanged(seekbar: SeekBar?, progress: Int, fromUser: Boolean) {
        channel.progress = progress
        progressView.text = progress.toString()
        listener?.invoke()
      }
    })
  }

  fun registerListener(listener: () -> Unit): Unit {
    this.listener = listener
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    this.listener = null
  }
}