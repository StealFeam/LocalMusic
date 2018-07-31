package com.zy.ppmusic.mvp.view

import android.app.Dialog
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Process
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import com.zy.ppmusic.R
import com.zy.ppmusic.utils.DateUtil
import com.zy.ppmusic.utils.FileUtils
import com.zy.ppmusic.utils.StreamUtils
import java.io.File
import java.io.PrintWriter

/**
 * @author y-slience
 * @date 2018-04-03 15:04:35
 * 错误页面，用来处理应用崩溃显示友好界面
 */
class ErrorActivity : AppCompatActivity() {
    companion object {
        const val ERROR_INFO = "ERROR_INFO"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.decorView.setBackgroundColor(Color.WHITE)

        writeMsgToLocal()

        val dialog = ErrorDialog()
        dialog.show(supportFragmentManager, ERROR_INFO)
    }

    private fun writeMsgToLocal() {
        val errorInfo = intent.getSerializableExtra(ERROR_INFO) as Throwable
        val writer = PrintWriter(File(FileUtils.downloadFile +"/music_error_log.txt"))
        writer.println("---- "+DateUtil.get().getTime(System.currentTimeMillis())+" ----")
        errorInfo.printStackTrace(writer)
        writer.flush()
        writer.close()
        StreamUtils.closeIo(writer)
    }

    class ErrorDialog : AppCompatDialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context!!)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.dialog_error_content_msg)
                        .setNegativeButton(R.string.dialog_error_sure) { _, _->
                            activity?.finish()
                            dismiss()
                            Process.killProcess(Process.myPid())
                            System.exit(0)
                        }.create()
    }
}
