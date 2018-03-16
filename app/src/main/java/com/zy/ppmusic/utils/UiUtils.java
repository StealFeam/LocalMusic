package com.zy.ppmusic.utils;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import com.zy.ppmusic.App;

/**
 * @author ZhiTouPC
 * @date 2017/10/19
 */

public class UiUtils {
    public static int dp2px(Context context,int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    public static int px2dp(Context context,int px) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (px / density + 0.5f);
    }

    public static String getString(int id){
        return App.getInstance().getResources().getString(id);
    }

    public static int getColor(int id){
        return ContextCompat.getColor(App.getInstance(),id);
    }
}
