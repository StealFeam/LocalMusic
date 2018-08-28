package com.zy.ppmusic;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;

import com.squareup.leakcanary.LeakCanary;
import com.zy.ppmusic.utils.CrashHandler;

/**
 * @author ZhiTouPC
 */
public class App extends Application {

    public static App getInstance() {
        return mAppInstance;
    }

    private static App mAppInstance;

    public static void setCustomDensity(@NonNull Activity activity) {
        final Application application = App.getInstance();
        final DisplayMetrics appDisplayMetrics = application.getResources().getDisplayMetrics();

        //px = density * dp
        //density= dpi / 160
        //px = dp * (dpi / 160)

        //以360dpi
        final float targetDensity = appDisplayMetrics.widthPixels / 360;
        final int targetDensityDpi = (int) (160 * targetDensity);

        appDisplayMetrics.density = targetDensity;
        appDisplayMetrics.densityDpi = targetDensityDpi;
        appDisplayMetrics.scaledDensity = targetDensity;

        final DisplayMetrics activityDisplayMetrics = activity.getResources().getDisplayMetrics();
        activityDisplayMetrics.density = activityDisplayMetrics.scaledDensity = targetDensity;
        activityDisplayMetrics.densityDpi = targetDensityDpi;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mAppInstance = this;
        if (!BuildConfig.IS_DEBUG) {
            CrashHandler handler = new CrashHandler(this);
            handler.attach();
        } else {
            LeakCanary.install(this);
        }
    }

    public Context getContext() {
        return this.getApplicationContext();
    }

}
