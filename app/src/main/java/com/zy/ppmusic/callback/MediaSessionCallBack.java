package com.zy.ppmusic.callback;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.ResultReceiver;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.zy.ppmusic.data.db.DBManager;
import com.zy.ppmusic.entity.MusicDbEntity;
import com.zy.ppmusic.service.MediaService;
import com.zy.ppmusic.utils.DataTransform;
import com.zy.ppmusic.utils.PrintOut;

import java.util.List;
import java.util.Objects;

import static com.zy.ppmusic.service.MediaService.ACTION_COUNT_DOWN_END;
import static com.zy.ppmusic.service.MediaService.ACTION_COUNT_DOWN_TIME;
import static com.zy.ppmusic.service.MediaService.ACTION_PARAM;
import static com.zy.ppmusic.service.MediaService.ACTION_STOP_COUNT_DOWN;
import static com.zy.ppmusic.service.MediaService.ERROR_PLAY_QUEUE_EVENT;

/**
 * @author ZhiTouPC
 * @date 2018/1/19
 */

public class MediaSessionCallBack extends MediaSessionCompat.Callback{
    private static final String TAG = "MediaSessionCallBack";
    private MediaService mService;


    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras) {
        super.onPlayFromMediaId(mediaId, extras);
        Log.d(TAG, "onPlayFromMediaId() called with: mediaId = [" + mediaId + "]");
    }

    @Override
    public void onPlay() {
        super.onPlay();
        Log.d(TAG, "onPlay() called");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() called");
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onSkipToQueueItem(long id) {
        super.onSkipToQueueItem(id);
    }

    @Override
    public void onSkipToNext() {
        Log.d(TAG, "onSkipToNext() called");
    }

    @Override
    public void onSkipToPrevious() {
        Log.d(TAG, "onSkipToPrevious() called");
    }

    @Override
    public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
        return true;
    }

    @Override
    public void onCommand(String command, Bundle reqExtra, ResultReceiver cb) {
        super.onCommand(command, reqExtra, cb);
    }

    @Override
    public void onSetRepeatMode(int repeatMode) {
        super.onSetRepeatMode(repeatMode);
    }

    @Override
    public void onRemoveQueueItem(MediaDescriptionCompat description) {
        super.onRemoveQueueItem(description);
        Log.d(TAG, "onRemoveQueueItem() called with: description = [" + description + "]");
    }

    @Override
    public void onCustomAction(String action, Bundle extras) {
        super.onCustomAction(action, extras);
    }
}
