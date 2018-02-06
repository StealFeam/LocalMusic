package com.zy.ppmusic.utils;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author ZhiTouPC
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    private final Context mContext;
    private ExecutorService mThreadPool;

    public CrashHandler(Context context) {
        this.mContext = context;
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
        if (mThreadPool == null) {
            mThreadPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingDeque<Runnable>(), new ThreadFactory() {
                @Override
                public Thread newThread(@NonNull Runnable r) {
                    return new Thread(r,TAG);
                }
            });
        }
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "sorry，发生错误了", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        });
        try {
            Thread.sleep(800);
        } catch (InterruptedException e1) {
            Log.e(TAG,e1.toString());
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
}
