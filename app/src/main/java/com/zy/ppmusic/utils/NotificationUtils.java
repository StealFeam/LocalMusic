package com.zy.ppmusic.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.zy.ppmusic.R;
import com.zy.ppmusic.service.MediaService;

public class NotificationUtils {
    private static final String TAG = "NotificationUtils";

    public static Notification postNotify(Context c, MediaSessionCompat mediaSession, boolean isPlaying) {
        Context context = c.getApplicationContext();
        NotificationManager manager = (NotificationManager) context.getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification;
        MediaControllerCompat controller = mediaSession.getController();//获取媒体控制器
        MediaMetadataCompat metadata = controller.getMetadata();//media信息
        PlaybackStateCompat playbackState = controller.getPlaybackState();//播放状态
        if (metadata == null || playbackState == null) {
            Log.e(TAG, "postNotification: " + (metadata == null) + "," + (playbackState == null));
            return null;
        }
        MediaDescriptionCompat descriptionCompat = metadata.getDescription();
        Bitmap iconBitmap = null;
        if (descriptionCompat != null) {
            iconBitmap = descriptionCompat.getIconBitmap();
        }
        if (iconBitmap == null) {
            iconBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_music_normal_round);
        }
        //添加内容布局
        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notify_copy_layout);
        contentView.setImageViewBitmap(R.id.notify_artist_head_iv, iconBitmap);
        if (isPlaying) {
            contentView.setImageViewResource(R.id.notify_artist_head_iv,R.drawable.ic_music_play);
            contentView.setImageViewResource(R.id.notify_action_play_pause, R.drawable.ic_pause);
        } else {
            contentView.setImageViewResource(R.id.notify_artist_head_iv,R.drawable.ic_music_normal_round);
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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_small_notify);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setCustomContentView(contentView);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        builder.setContentIntent(mediaSession.getController().getSessionActivity());

        notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        if(manager != null){
            manager.notify(MediaService.NOTIFY_ID, notification);
        }
        return notification;
    }


}
