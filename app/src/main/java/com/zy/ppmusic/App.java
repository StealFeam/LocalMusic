package com.zy.ppmusic;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.StrictMode;

import com.squareup.leakcanary.LeakCanary;
import com.zy.ppmusic.utils.CrashHandler;

/**
 * @author ZhiTouPC
 */
public class App extends Application {
    public static final String LOCAL_DATA_TABLE_NAME = "CACHE_PATH_LIST";

    public static class SingleInstance{
        public static App app = new App();
    }

    public static App getInstance(){
        return SingleInstance.app;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.enableDefaults();
        LeakCanary.install(this);

//        CrashHandler handler = new CrashHandler(this);
    }

    public static int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }


    public Context getContext(){
        return this.getApplicationContext();
    }

}
