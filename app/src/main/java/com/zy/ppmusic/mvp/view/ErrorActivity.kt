package com.zy.ppmusic.mvp.view

import android.app.Dialog
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Process
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import com.zy.ppmusic.R
import com.zy.ppmusic.utils.PrintLog

/**
 * @author y-slience
 * @date 2018-04-03 15:04:35
 * 错误页面，用来处理应用崩溃显示友好界面
 */
class ErrorActivity : AppCompatActivity() {
    companion object {
        val ERROR_INFO = "ERROR_INFO"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.decorView.setBackgroundColor(Color.WHITE)

        val errorInfo = intent.getSerializableExtra(ERROR_INFO) as Throwable
        errorInfo.printStackTrace()

        val dialog = ErrorDialog()
        dialog.show(supportFragmentManager,"error")
    }

    class ErrorDialog : AppCompatDialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context!!)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.dialog_error_content_msg)
                        .setNegativeButton(R.string.dialog_error_sure, { _, _->
                            activity?.finish()
                            dismiss()
                            Process.killProcess(Process.myPid())
                            System.exit(0)
                        }).create()
    }
}
