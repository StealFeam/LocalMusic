package com.zy.ppmusic.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtil {

    public static List<String> checkPermission(Context context, String... permission) {
        List<String> needList = new ArrayList<>();
        for (String item : permission) {
            if (ContextCompat.checkSelfPermission(context, item) != PackageManager.PERMISSION_GRANTED) {
                needList.add(item);
            }
        }
        return needList;
    }
}
