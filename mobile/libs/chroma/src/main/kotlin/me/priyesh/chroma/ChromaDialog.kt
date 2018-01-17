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

package me.priyesh.chroma

import android.app.AlertDialog
import android.app.Dialog
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v4.app.DialogFragment
import android.view.WindowManager
import me.priyesh.chroma.internal.ChromaView
import kotlin.properties.Delegates

class ChromaDialog constructor() : DialogFragment() {

  companion object {
    private val ArgInitialColor = "arg_initial_color"
    private val ArgColorModeName = "arg_color_mode_name"

    @JvmStatic
    private fun newInstance(@ColorInt initialColor: Int, colorMode: ColorMode): ChromaDialog {
      val fragment = ChromaDialog()
      fragment.arguments = makeArgs(initialColor, colorMode)
      return fragment
    }

    @JvmStatic
    private fun makeArgs(@ColorInt initialColor: Int, colorMode: ColorMode): Bundle {
      val args = Bundle()
      args.putInt(ArgInitialColor, initialColor)
      args.putString(ArgColorModeName, colorMode.name)
      return args
    }
  }

  class Builder {
    @ColorInt private var initialColor: Int = ChromaView.DefaultColor
    private var colorMode: ColorMode = ChromaView.DefaultModel
    private var listener: ColorSelectListener? = null

    fun initialColor(@ColorInt initialColor: Int): Builder {
      this.initialColor = initialColor
      return this
    }

    fun colorMode(colorMode: ColorMode): Builder {
      this.colorMode = colorMode
      return this
    }

    fun onColorSelected(listener: ColorSelectListener): Builder {
      this.listener = listener
      return this
    }

    fun create(): ChromaDialog {
      val fragment = newInstance(initialColor, colorMode)
      fragment.listener = listener
      return fragment
    }
  }

  private var listener: ColorSelectListener? = null
  private var chromaView: ChromaView by Delegates.notNull<ChromaView>()

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    chromaView = if (savedInstanceState == null) {
      ChromaView(
          arguments.getInt(ArgInitialColor),
          ColorMode.fromName(arguments.getString(ArgColorModeName)),
          context)
    } else {
      ChromaView(
          savedInstanceState.getInt(ArgInitialColor, ChromaView.DefaultColor),
          ColorMode.fromName(savedInstanceState.getString(ArgColorModeName)),
          context
      )
    }

    chromaView.enableButtonBar(object : ChromaView.ButtonBarListener {
      override fun onNegativeButtonClick() = dismiss()
      override fun onPositiveButtonClick(color: Int) {
        listener?.onColorSelected(color)
        dismiss()
      }
    })

    return AlertDialog.Builder(context).setView(chromaView).create().apply {
      setOnShowListener {
        val width: Int; val height: Int
        if (orientation(context) == ORIENTATION_LANDSCAPE) {
          height = resources.getDimensionPixelSize(R.dimen.chroma_dialog_height)
          width = 80 percentOf screenDimensions(context).widthPixels
        } else {
          height = WindowManager.LayoutParams.WRAP_CONTENT
          width = resources.getDimensionPixelSize(R.dimen.chroma_dialog_width)
        }
        window.setLayout(width, height)
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle?) {
    outState?.putAll(makeArgs(chromaView.currentColor, chromaView.colorMode))
    super.onSaveInstanceState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    listener = null
  }
}
