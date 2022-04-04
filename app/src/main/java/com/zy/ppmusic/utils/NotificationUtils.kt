package com.zy.ppmusic.utils

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.zy.ppmusic.R
import com.zy.ppmusic.mvp.view.MediaActivity

/**
 * @author stealfeam
 */
object NotificationUtils {
    private const val TAG = "NotificationUtils"
    /**
     * 通知的id
     */
    const val NOTIFY_ID = 40012
    private const val NOTIFY_CHANNEL_ID = "notify_channel_id"

    private var CURRENT_STYLE = -1

    fun getNotifyStyle(context: Context): Int {
        var style = CURRENT_STYLE
        if (style == -1) {
            val sp = context.getSharedPreferences(Constant.LOCAL_CHOOSE_FILE, MODE_PRIVATE)
            style = sp.getInt(Constant.LOCAL_STYLE_NAME, R.id.rb_choose_custom)
        }
        return style
    }

    fun setNotifyStyle(style: Int) {
        if (style == CURRENT_STYLE) {
            return
        }
        CURRENT_STYLE = style
    }

    fun createSystemNotify(c: Context, mediaSession: MediaSessionCompat?, isPlaying: Boolean): Notification? {
        val context = c.applicationContext
        if (mediaSession == null)
            return null
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return null
        }
        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val descriptionCompat = mediaSession.controller?.metadata?.description

        val builder = initBuilder(context, mNotificationManager)
        builder.setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
        builder.setContentIntent(mediaSession.controller.sessionActivity)
        builder.setDeleteIntent(handleBuildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
        builder.setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
        builder.setSmallIcon(R.drawable.ic_small_notify)
        builder.setColorized(true)

        builder.addAction(
            R.drawable.ic_previous,
            "上一首",
            handleBuildMediaButtonPendingIntent(
                context,
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
        )
        //设置显示的按钮信息
        if (isPlaying) {
            builder.addAction(
                R.drawable.ic_system_style_pause,
                "暂停",
                handleBuildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE)
            )
        } else {
            builder.addAction(
                R.drawable.ic_system_style_play,
                "播放",
                handleBuildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY)
            )
        }
        builder.addAction(
            R.drawable.ic_system_style_next,
            "下一首",
            handleBuildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        )
        builder.addAction(
            R.drawable.ic_system_style_stop,
            "关闭",
            handleBuildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)
        )

        val mediaStyle: NotificationCompat.MediaStyle = NotificationCompat.MediaStyle()
        mediaStyle.setMediaSession(mediaSession.sessionToken)
        mediaStyle.setShowActionsInCompactView(1, 2, 3)

        builder.setOnlyAlertOnce(true)
        builder.setStyle(mediaStyle)
        builder.setShowWhen(false)

        if (descriptionCompat != null) {
            Log.e(TAG, "postNotification: title=" + descriptionCompat.title +
                    ",subTitle=" + descriptionCompat.subtitle)
            builder.setSubText(context.getString(R.string.notification_sub_text))
            builder.setContentTitle(descriptionCompat.title)
            builder.setContentText(descriptionCompat.subtitle)
        } else {
            Log.e(TAG, "postNotification: description is null")
        }
        if (descriptionCompat != null) {
            if (descriptionCompat.iconBitmap == null) {
                builder.setLargeIcon(BitmapFactory.decodeResource(context.resources,
                        R.drawable.ic_music_normal_round))
            } else {
                builder.setLargeIcon(descriptionCompat.iconBitmap)
            }
        } else {
            builder.setLargeIcon(BitmapFactory.decodeResource(context.resources,
                    R.drawable.ic_music_normal_round))
        }
        return builder.build()
    }

    /**
     * Android S设备及以上都需要将PendingIntent的flag设置为FLAG_IMMUTABLE
     * 但是 #MediaButtonReceiver.buildMediaButtonPendingIntent并没有设置
     * 所以会崩溃，这里拷贝的系统的代码修复了问题
     */
    private fun handleBuildMediaButtonPendingIntent(context: Context, action: Long): PendingIntent? {
        return if (belowSVersion) {
            MediaButtonReceiver.buildMediaButtonPendingIntent(context, action)
        } else {
            val keyCode = PlaybackStateCompat.toKeyCode(action)
            if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
                Log.w(TAG, "Cannot build a media button pending intent with the given action: $action")
                null
            } else {
                val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
                intent.component = getMediaButtonReceiverComponent(context)
                intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                PendingIntent.getBroadcast(context, keyCode, intent, PendingIntent.FLAG_IMMUTABLE)
            }
        }
    }

    private fun getMediaButtonReceiverComponent(context: Context): ComponentName? {
        val queryIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        queryIntent.setPackage(context.packageName)
        val pm = context.packageManager
        val resolveInfos = pm.queryBroadcastReceivers(queryIntent, 0)
        if (resolveInfos.size == 1) {
            val resolveInfo = resolveInfos[0]
            return ComponentName(
                resolveInfo.activityInfo.packageName,
                resolveInfo.activityInfo.name
            )
        } else if (resolveInfos.size > 1) {
            Log.w(
                TAG,
                "More than one BroadcastReceiver that handles "
                    + Intent.ACTION_MEDIA_BUTTON + " was found, returning null."
            )
        }
        return null
    }

    fun createCustomNotify(c: Context, sessionCompat: MediaSessionCompat?, isPlaying: Boolean): Notification? {
        val context = c.applicationContext
        if (sessionCompat == null)
            return null
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return null
        }
        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val descriptionCompat = sessionCompat.controller?.metadata?.description
        val iconBitmap: Bitmap? = descriptionCompat?.iconBitmap
        //添加内容布局
        val contentView = RemoteViews(context.packageName, R.layout.notify_copy_layout)

        contentView.setImageViewBitmap(R.id.notify_artist_head_iv, iconBitmap)

        if (isPlaying) {
            if (iconBitmap == null) {
                contentView.setImageViewResource(R.id.notify_artist_head_iv, R.drawable.ic_music_play)
            } else {
                contentView.setImageViewBitmap(R.id.notify_artist_head_iv, iconBitmap)
            }
            contentView.setImageViewResource(R.id.notify_action_play_pause, R.drawable.ic_pause)
        } else {
            if (iconBitmap == null) {
                contentView.setImageViewResource(R.id.notify_artist_head_iv, R.drawable.ic_music_normal_round)
            } else {
                contentView.setImageViewBitmap(R.id.notify_artist_head_iv, iconBitmap)
            }
            contentView.setImageViewResource(R.id.notify_action_play_pause, R.drawable.ic_play)
        }

        if (descriptionCompat != null) {
            Log.e(TAG, "postNotification: title=" + descriptionCompat.title + ",subTitle=" + descriptionCompat.subtitle)
            contentView.setTextViewText(R.id.notify_display_title, StringUtils.ifEmpty(descriptionCompat.title.toString(), getString(R.string.unknown_name)))
            contentView.setTextViewText(R.id.notify_display_sub_title, StringUtils.ifEmpty(descriptionCompat.subtitle.toString(), getString(R.string.unknown_author)))
        } else {
            Log.e(TAG, "postNotification: description is null")
        }
        contentView.setImageViewResource(R.id.notify_action_next,R.drawable.ic_next)
        contentView.setImageViewResource(R.id.notify_action_close,R.mipmap.ic_black_close)

        contentView.setOnClickPendingIntent(
            R.id.notify_action_play_pause,
            handleBuildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE)
        )
        contentView.setOnClickPendingIntent(
            R.id.notify_action_next,
            handleBuildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        )
        contentView.setOnClickPendingIntent(
            R.id.notify_action_close,
            handleBuildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)
        )

        val builder = initBuilder(context, mNotificationManager)
        builder.setContent(contentView)
        builder.setContentIntent(sessionCompat.controller.sessionActivity)
        builder.setDeleteIntent(handleBuildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
        builder.setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
        builder.setSmallIcon(R.drawable.ic_small_notify)

        return builder.build()
    }

    /**
     * 适配Android O 平台的通知通道
     *
     * @param context             环境变量
     * @param notificationManager 通知管理器
     * @return 生成好的builder
     */
    private fun initBuilder(context: Context, notificationManager: NotificationManager): androidx.core.app.NotificationCompat.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //获取通道
            var notificationChannel: NotificationChannel? = notificationManager.getNotificationChannel(TAG)
            if (notificationChannel == null) {
                val group = NotificationChannelGroup(NOTIFY_CHANNEL_ID, context.getString(R.string.app_name))
                notificationManager.createNotificationChannelGroup(group)

                notificationChannel = NotificationChannel(NOTIFY_CHANNEL_ID, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT)

                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                notificationChannel.group = group.id
                notificationChannel.setSound(null, null)
                notificationChannel.enableLights(false)
                notificationChannel.enableVibration(false)
                notificationChannel.setShowBadge(false)
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }
        return androidx.core.app.NotificationCompat.Builder(context, NOTIFY_CHANNEL_ID)
    }

    fun cancelNotify(context: Context, notifyId: Int) {
        val managerCompat = NotificationManagerCompat.from(context)
        managerCompat.cancel(notifyId)
    }

    fun cancelAllNotify(context: Context) {
        val managerCompat = NotificationManagerCompat.from(context)
        managerCompat.cancelAll()
    }
}
