package com.zy.ppmusic;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;

import com.squareup.leakcanary.LeakCanary;
import com.zy.ppmusic.utils.CrashHandler;
import com.zy.ppmusic.utils.PrintLog;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * @author ZhiTouPC
 */
public class App extends Application {
    public static final String LOCAL_DATA_TABLE_NAME = "CACHE_PATH_LIST";
    private LinkedHashMap<String,WeakReference<Activity>> mActivityLists = new LinkedHashMap<>();

    public static App getInstance() {
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


    public static void setCustomDensity(@NonNull Activity activity) {
        final DisplayMetrics appDisplayMetrics = App.getInstance().getResources().getDisplayMetrics();

        //px = density * dp
        //density= dpi / 160
        //px = dp * (dpi / 160)

        //以360dpi
        final float targetDensity = appDisplayMetrics.widthPixels / 360;
        final int targetDensityDpi = (int) (160 * targetDensity);

        appDisplayMetrics.density = targetDensity;
        appDisplayMetrics.densityDpi = targetDensityDpi;

        final DisplayMetrics activityDisplayMetrics = activity.getResources().getDisplayMetrics();
        activityDisplayMetrics.density = activityDisplayMetrics.scaledDensity = targetDensity;
        activityDisplayMetrics.densityDpi = targetDensityDpi;

    }


    @Override
    public void onCreate() {
        super.onCreate();
        mAppInstance = this;
        StrictMode.enableDefaults();
        LeakCanary.install(this);
        if (!BuildConfig.IS_DEBUG) {
            CrashHandler handler = new CrashHandler(this);
            handler.attach();
        }
    }

    public Context getContext() {
        return this.getApplicationContext();
    }

    public void createActivity(Activity activity) {
        mActivityLists.put(activity.getLocalClassName(), new WeakReference<>(activity));
    }

    public void destroyActivity(Activity activity) {
        if (!mActivityLists.containsKey(activity.getLocalClassName())) {
            PrintLog.INSTANCE.e("not found this activity " + activity.getLocalClassName());
            return;
        }
        WeakReference<Activity> activityWeakReference = mActivityLists.get(activity.getLocalClassName());
        if (activityWeakReference.get() != null) {
            activityWeakReference.clear();
        }
    }

    public void killSelf() {
        Iterator<WeakReference<Activity>> iterator = mActivityLists.values().iterator();
        while (iterator.hasNext()) {
            WeakReference<Activity> activityWeakReference = iterator.next();
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
