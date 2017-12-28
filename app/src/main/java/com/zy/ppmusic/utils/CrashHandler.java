package com.zy.ppmusic.utils;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    public CrashHandler(Context context) {
        this.mContext = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
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
        PrintOut.print(e.getMessage());
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "sorry，发生错误了", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }).start();
        try {
            Thread.sleep(800);
        } catch (InterruptedException e1) {
            Log.e(TAG,e1.toString());
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
}
