package com.zy.ppmusic;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;

import com.squareup.leakcanary.LeakCanary;
import com.zy.ppmusic.utils.Constant;
import com.zy.ppmusic.utils.CrashHandler;
import com.zy.ppmusic.utils.SpUtils;

import java.lang.ref.WeakReference;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

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

    private static WeakReference<Context> baseContext;

    public static Context getAppBaseContext(){
        return baseContext.get();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        baseContext = new WeakReference<>(base);
        SpUtils.get().putOperator(new Function1<SharedPreferences.Editor, Unit>() {
            @Override
            public Unit invoke(SharedPreferences.Editor editor) {
                editor.putLong(Constant.SP_APP_ATTACH_TIME,System.currentTimeMillis());
                return null;
            }
        });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAppInstance = this;
        if (!BuildConfig.IS_DEBUG) {
            CrashHandler handler = new CrashHandler(this);
            handler.attach();
            Debug.stopMethodTracing();
        } else {
            LeakCanary.install(this);
        }
    }

    public Context getContext() {
        return this.getApplicationContext();
    }

}
