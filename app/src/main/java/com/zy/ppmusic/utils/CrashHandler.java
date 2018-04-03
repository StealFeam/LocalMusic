package com.zy.ppmusic.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.zy.ppmusic.R;
import com.zy.ppmusic.mvp.view.BlScanActivity;
import com.zy.ppmusic.mvp.view.ErrorActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author ZhiTouPC
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private final Context mContext;

    public CrashHandler(Context context) {
        this.mContext = context;
    }

    public void attach(){
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * Method invoked when the given thread terminates due to the
     * given uncaught exception.
     * <p>Any exception thrown by this method will be ignored by the
     * Java Virtual Machine.
     *
     * @param t the thread
     * @param e the exception
     */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (e == null) {
            return;
        }
        PrintLog.print(e.getMessage());
        Intent it = new Intent(mContext, ErrorActivity.class);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivity(it);
        Process.killProcess(Process.myPid());
    }
}
