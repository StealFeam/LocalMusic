package com.zy.ppmusic.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.zy.ppmusic.R;

import java.util.Currency;

import static android.content.Context.MODE_PRIVATE;

/**
 * @author ZhiTouPC
 */
public class NotificationUtils {
    private static final String TAG = "NotificationUtils";
    /**
     * 通知的id
     */
    public static final int NOTIFY_ID = 40012;

    private static int CURRENT_STYLE = -1;


    public static int getNotifyStyle(Context context){
        int style = CURRENT_STYLE;
        if(style == -1){
            SharedPreferences sp = context.getSharedPreferences(Constant.LOCAL_CHOOSE_FILE,MODE_PRIVATE);
            style = sp.getInt(Constant.LOCAL_STYLE_NAME, R.id.rb_choose_custom);
        }
        return style;
    }

    public static void setNotifyStyle(int style){
        if(style == CURRENT_STYLE){
            return;
        }
        CURRENT_STYLE = style;
    }


    public static Notification createSystemNotify(Context c, MediaSessionCompat mediaSession, boolean isPlaying) {
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

        NotificationCompat.Builder builder = initBuilder(context,mNotificationManager);
        if (descriptionCompat != null) {
            Log.e(TAG, "postNotification: title=" + descriptionCompat.getTitle() +
                    ",subTitle=" + descriptionCompat.getSubtitle());
            builder.setContentTitle(descriptionCompat.getTitle());
            builder.setContentText(descriptionCompat.getSubtitle());
            builder.setSubText(descriptionCompat.getDescription());
        } else {
            Log.e(TAG, "postNotification: description is null");
        }

        if (descriptionCompat != null) {
            if (descriptionCompat.getIconBitmap() == null) {
                builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.ic_music_normal_round));
            } else {
                builder.setLargeIcon(descriptionCompat.getIconBitmap());
            }
        } else {
            builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_music_normal_round));
        }

        builder.setContentIntent(mediaSession.getController().getSessionActivity());
        builder.setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                PlaybackStateCompat.ACTION_STOP));
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setSmallIcon(R.drawable.ic_small_notify);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            builder.setColorized(true);
            builder.setColor(ContextCompat.getColor(context,R.color.colorBlack));
        }else{
            builder.setColorized(true);
            builder.setColor(ContextCompat.getColor(context,R.color.colorTheme));
        }

        //设置显示的按钮信息
        if (isPlaying) {
            builder.addAction(R.drawable.ic_system_style_pause, "暂停",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                            PlaybackStateCompat.ACTION_PLAY_PAUSE));
        } else {
            builder.addAction(R.drawable.ic_system_style_play, "播放",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                    PlaybackStateCompat.ACTION_PLAY_PAUSE));
        }

        builder.addAction(R.drawable.ic_system_style_next, "下一首",
                MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
        builder.addAction(R.drawable.ic_system_style_stop, "关闭",
                MediaButtonReceiver.buildMediaButtonPendingIntent(context,
                PlaybackStateCompat.ACTION_STOP));

        android.support.v4.media.app.NotificationCompat.MediaStyle mediaStyle;
        mediaStyle = new android.support.v4.media.app.NotificationCompat.MediaStyle();
        mediaStyle.setBuilder(builder);
        mediaStyle.setMediaSession(mediaSession.getSessionToken());
        mediaStyle.setShowActionsInCompactView(0, 1, 2);

        Notification notification = mediaStyle.build();

        notification.flags |= Notification.FLAG_NO_CLEAR;

        return notification;
    }

    public static Notification createCustomNotify(Context c,MediaSessionCompat sessionCompat,boolean isPlaying){
        Context context = c.getApplicationContext();
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);

        if (mNotificationManager == null) {
            return null;
        }

        //获取媒体控制器
        MediaControllerCompat controller = sessionCompat.getController();
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

//        添加内容布局
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
            Log.e(TAG, "postNotification: title=" + descriptionCompat.getTitle() +
                    ",subTitle=" + descriptionCompat.getSubtitle());
            contentView.setTextViewText(R.id.notify_display_title, StringUtils.Companion
                    .ifEmpty(String.valueOf(descriptionCompat.getTitle()), UiUtils.getString(R.string.unknown_name)));
            contentView.setTextViewText(R.id.notify_display_sub_title, StringUtils.Companion
                    .ifEmpty(String.valueOf(descriptionCompat.getSubtitle()), UiUtils.getString(R.string.unknown_author)));
        } else {
            Log.e(TAG, "postNotification: description is null");
        }
        contentView.setOnClickPendingIntent(R.id.notify_action_play_pause, MediaButtonReceiver.
                buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE));
        contentView.setOnClickPendingIntent(R.id.notify_action_next, MediaButtonReceiver.
                buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
        contentView.setOnClickPendingIntent(R.id.notify_action_close, MediaButtonReceiver.
                buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP));
        NotificationCompat.Builder builder = initBuilder(context,mNotificationManager);
        if (descriptionCompat != null) {
            Log.e(TAG, "postNotification: title=" + descriptionCompat.getTitle() +
                    ",subTitle=" + descriptionCompat.getSubtitle());
            builder.setContentTitle(descriptionCompat.getTitle());
            builder.setContentText(descriptionCompat.getSubtitle());
            builder.setSubText(descriptionCompat.getDescription());
        } else {
            Log.e(TAG, "postNotification: description is null");
        }

        builder.setContent(contentView);
        builder.setContentIntent(controller.getSessionActivity());
        builder.setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP));
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setSmallIcon(R.drawable.ic_small_notify);

        Notification notification = builder.build();

        notification.flags |= Notification.FLAG_NO_CLEAR;

        return notification;
    }

    /**
     * 适配Android O 平台的通知通道
     * @param context 环境变量
     * @param notificationManager 通知管理器
     * @return 生成好的builder
     */
    private static NotificationCompat.Builder initBuilder(Context context,NotificationManager notificationManager){
        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //获取通道
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(TAG);
            if (notificationChannel == null) {
                notificationChannel = new NotificationChannel("音乐",
                        "播放通知", NotificationManager.IMPORTANCE_DEFAULT);
                notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                //不显示图标上的小圆点
                notificationChannel.setShowBadge(false);
                notificationChannel.setSound(null, null);
                //取消震动
                notificationChannel.enableVibration(false);
                //取消提示灯
                notificationChannel.enableLights(false);
                notificationManager.createNotificationChannel(notificationChannel);
            }

            //用户关闭了通知通道
            if (notificationChannel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                Intent it = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                it.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                it.putExtra(Settings.EXTRA_CHANNEL_ID, notificationChannel.getId());
                context.startActivity(it);
                Toast.makeText(context, "请将通知权限打开", Toast.LENGTH_SHORT).show();
            }
            builder = new NotificationCompat.Builder(context, notificationChannel.getId());
        } else {
            builder = new NotificationCompat.Builder(context, String.valueOf(NOTIFY_ID));
        }
        return builder;
    }


    public static void cancelNotify(Context context, int notifyId) {
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        managerCompat.cancel(notifyId);
    }

    public static void cancelAllNotify(Context context) {
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        managerCompat.cancelAll();
    }
}
