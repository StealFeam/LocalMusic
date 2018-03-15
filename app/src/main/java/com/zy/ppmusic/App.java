package com.zy.ppmusic;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;

import com.squareup.leakcanary.LeakCanary;
import com.zy.ppmusic.utils.PrintOut;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * @author ZhiTouPC
 */
public class App extends Application {
    public static final String LOCAL_DATA_TABLE_NAME = "CACHE_PATH_LIST";
    private static LinkedHashMap<String, WeakReference<AppCompatActivity>> mActivityLists;

    public static App getInstance() {
        mActivityLists = new LinkedHashMap<>();
        return mAppInstance;
    }

    private static App mAppInstance;

    public static int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAppInstance = this;
        StrictMode.enableDefaults();
        LeakCanary.install(this);
    }

    public Context getContext() {
        return this.getApplicationContext();
    }

    public void createActivity(AppCompatActivity activity) {
        mActivityLists.put(activity.getLocalClassName(), new WeakReference<>(activity));
    }

    public void destroyActivity(AppCompatActivity activity) {
        if (!mActivityLists.containsKey(activity.getLocalClassName())) {
            PrintOut.e("not found this activity " + activity.getLocalClassName());
            return;
        }
        WeakReference<AppCompatActivity> activityWeakReference = mActivityLists.get(activity.getLocalClassName());
        if (activityWeakReference.get() != null) {
            activityWeakReference.clear();
        }
    }

    public void killSelf() {
        Iterator<WeakReference<AppCompatActivity>> iterator = mActivityLists.values().iterator();
        while (iterator.hasNext()) {
            WeakReference<AppCompatActivity> activityWeakReference = iterator.next();
            if (activityWeakReference.get() != null) {
                activityWeakReference.get().finish();
            }
            activityWeakReference.clear();
            iterator.remove();
        }
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
        String command = "kill -9 " + pid;
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
