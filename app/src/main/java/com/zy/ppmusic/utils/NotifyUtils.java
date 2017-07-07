package com.zy.ppmusic.utils;

import android.app.Notification;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;

public class NotifyUtils {
    private static class  Instance{
        private static NotifyUtils getSingle(){
            return new NotifyUtils();
        }
    }

    public static NotifyUtils getInstance(){
        return Instance.getSingle();
    }

    public void notifyMusic(Context context, Notification compat){
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        managerCompat.notify(0,compat);
    }
}
