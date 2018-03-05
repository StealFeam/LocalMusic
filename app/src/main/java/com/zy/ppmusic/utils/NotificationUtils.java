package com.zy.ppmusic.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.zy.ppmusic.R;

/**
 * @author ZhiTouPC
 */
public class NotificationUtils {
    private static final String TAG = "NotificationUtils";
    /**
     * 通知的id
     */
    public static final int NOTIFY_ID = 40012;

    public static Notification createNotify(Context c, MediaSessionCompat mediaSession, boolean isPlaying) {
        Context context = c.getApplicationContext();
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);

        if (mNotificationManager == null) {
            return null;
        }

        //获取媒体控制器
        MediaControllerCompat controller = mediaSession.getController();
        //media信息
        MediaMetadataCompat metadata = controller.getMetadata();
        //播放状态
        PlaybackStateCompat playbackState = controller.getPlaybackState();
        if (metadata == null || playbackState == null) {
            Log.e(TAG, "postNotification: " + (metadata == null) + "," + (playbackState == null));
            return null;
        }
        MediaDescriptionCompat descriptionCompat = metadata.getDescription();
        Bitmap iconBitmap = null;
        if (descriptionCompat != null) {
            iconBitmap = descriptionCompat.getIconBitmap();
        }

        //添加内容布局
        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notify_copy_layout);
        contentView.setImageViewBitmap(R.id.notify_artist_head_iv, iconBitmap);

        if (isPlaying) {
            if (iconBitmap == null) {
                contentView.setImageViewResource(R.id.notify_artist_head_iv, R.drawable.ic_music_play);
            } else {
                contentView.setImageViewBitmap(R.id.notify_artist_head_iv, iconBitmap);
            }
            contentView.setImageViewResource(R.id.notify_action_play_pause, R.drawable.ic_pause);
        } else {
            if (iconBitmap == null) {
                contentView.setImageViewResource(R.id.notify_artist_head_iv, R.drawable.ic_music_normal_round);
            } else {
                contentView.setImageViewBitmap(R.id.notify_artist_head_iv, iconBitmap);
            }
            contentView.setImageViewResource(R.id.notify_action_play_pause, R.drawable.ic_play);
        }
        if (descriptionCompat != null) {
            Log.e(TAG, "postNotification: title=" + descriptionCompat.getTitle() + ",subTitle=" + descriptionCompat.getSubtitle());
            contentView.setTextViewText(R.id.notify_display_title, descriptionCompat.getTitle());
            contentView.setTextViewText(R.id.notify_display_sub_title, descriptionCompat.getSubtitle());
        } else {
            Log.e(TAG, "postNotification: description is null");
        }
        contentView.setOnClickPendingIntent(R.id.notify_action_play_pause, MediaButtonReceiver.
                buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE));
        contentView.setOnClickPendingIntent(R.id.notify_action_next, MediaButtonReceiver.
                buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
        contentView.setOnClickPendingIntent(R.id.notify_action_close, MediaButtonReceiver.
                buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP));
        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //获取通道
            NotificationChannel notificationChannel = mNotificationManager.getNotificationChannel(TAG);
            if (notificationChannel == null) {
                notificationChannel = new NotificationChannel(TAG,
                        context.getPackageName(), NotificationManager.IMPORTANCE_DEFAULT);
                mNotificationManager.createNotificationChannel(notificationChannel);
            }
            builder = new NotificationCompat.Builder(context,notificationChannel.getId());
        }else{
            builder = new NotificationCompat.Builder(context,
                    String.valueOf(NOTIFY_ID));
        }
        builder.setSmallIcon(R.drawable.ic_small_notify);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setCustomContentView(contentView);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        builder.setContentIntent(mediaSession.getController().getSessionActivity());

        Notification notification = builder.build();

        notification.flags |= Notification.FLAG_NO_CLEAR;

        return notification;
    }

    public static void cancelNotify(Context context,int notifyId){
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        managerCompat.cancel(notifyId);
    }

    public static void cancelAllNotify(Context context){
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        managerCompat.cancelAll();
    }
}
