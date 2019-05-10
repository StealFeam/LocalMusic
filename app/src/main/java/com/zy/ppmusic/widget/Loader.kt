package com.zy.ppmusic.widget

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.WindowManager
import com.zy.ppmusic.utils.UIUtils

/**
 * @author Smith
 * @date：2019-05-10
 * @description：
 */
class Loader {
    companion object{
        @JvmStatic fun show(context: Context): Loader{
            return Loader(context)
        }
    }
    private lateinit var refreshView: WaveRefreshView

    private constructor()

    constructor(context: Context) {
        handleShow(context)
    }

    private fun handleShow(context: Context) {
        refreshView = WaveRefreshView(context)
        refreshView.setBackgroundColor(Color.TRANSPARENT)
        val params = WindowManager.LayoutParams(UIUtils.dp2px(context, 50), UIUtils.dp2px(context, 50),
            WindowManager.LayoutParams.TYPE_APPLICATION,WindowManager.LayoutParams.FLAG_FULLSCREEN,-3)
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        refreshView.startAnim()
        windowManager.addView(refreshView, params)
    }

    fun hide() {
        refreshView.stopRunningAnim()
        (refreshView.context as Activity).windowManager.removeView(refreshView)
    }
}

