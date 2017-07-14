package com.zy.ppmusic;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class App extends Application{
    public static final String LOCAL_DATA_TABLE_NAME = "CACHE_PATH_LIST";

    @Override
    public void onCreate() {
        super.onCreate();
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
}
