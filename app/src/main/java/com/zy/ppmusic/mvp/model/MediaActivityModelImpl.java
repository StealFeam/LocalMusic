package com.zy.ppmusic.mvp.model;

import android.content.Context;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.media.session.MediaControllerCompat;

import com.zy.ppmusic.mvp.contract.IMediaActivityContract;
import com.zy.ppmusic.utils.FileUtils;
import com.zy.ppmusic.utils.ScanMusicFile;

import java.lang.ref.WeakReference;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author ZhiTouPC
 */
public class MediaActivityModelImpl implements IMediaActivityContract.IMediaActivityModel {
    private static final String TAG = "MediaMediaActivityModel";
    private ThreadPoolExecutor mBackGroundPool;

    public MediaActivityModelImpl() {
        mBackGroundPool = new ThreadPoolExecutor(1, 4, 0L,
                TimeUnit.MICROSECONDS, new LinkedBlockingDeque<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, TAG);
            }
        });
    }

    @Override
    public void refreshQueue(Context context, ScanMusicFile.AbstractOnScanComplete complete) {
        ScanMusicFile.getInstance().setOnScanComplete(complete).scanMusicFile(context);
    }

    @Override
    public void loadLocalData(String path, final IOnLocalDataLoadFinished callback) {
        final String localPath = path;
        mBackGroundPool.execute(new Runnable() {
            @Override
            public void run() {
                Object localData = FileUtils.readObject(localPath);
                callback.callBack(localData);
            }
        });
    }

    @Override
    public void postSendCommand(MediaControllerCompat controller, final String method, final Bundle params,
                                ResultReceiver resultReceiver) {
        final WeakReference<MediaControllerCompat> mWeakController = new WeakReference<>(controller);
        final WeakReference<ResultReceiver> mWeakReceiver = new WeakReference<>(resultReceiver);
        mBackGroundPool.execute(new Runnable() {
            @Override
            public void run() {
                if(mWeakController.get() != null){
                    mWeakController.get().sendCommand(method,params,mWeakReceiver.get());
                }
            }
        });
    }

    @Override
    public void postPlayWithId(MediaControllerCompat controller, final String mediaId, final Bundle extra) {
        final WeakReference<MediaControllerCompat> mWeakController = new WeakReference<>(controller);
        mBackGroundPool.execute(new Runnable() {
            @Override
            public void run() {
                if(mWeakController.get() != null){
                    mWeakController.get().getTransportControls().playFromMediaId(mediaId,extra);
                }
                mWeakController.clear();
            }
        });
    }

    @Override
    public void postSendCustomAction(MediaControllerCompat controller, final String action, final Bundle extra) {
        final WeakReference<MediaControllerCompat> mWeakController = new WeakReference<>(controller);
//        mBackGroundPool.execute(new Runnable() {
//            @Override
//            public void run() {
                if(mWeakController.get() != null){
                    mWeakController.get().getTransportControls().sendCustomAction(action,extra);
                }
                mWeakController.clear();
//            }
//        });
    }

    @Override
    public void postSetRepeatMode(MediaControllerCompat controller, final int mode) {
        final WeakReference<MediaControllerCompat> mWeakController = new WeakReference<>(controller);
        mBackGroundPool.execute(new Runnable() {
            @Override
            public void run() {
                if(mWeakController.get() != null){
                    mWeakController.get().getTransportControls().setRepeatMode(mode);
                }
                mWeakController.clear();
            }
        });
    }

    @Override
    public void postSkipNext(MediaControllerCompat controller) {
        final WeakReference<MediaControllerCompat> mWeakController = new WeakReference<>(controller);
        mBackGroundPool.execute(new Runnable() {
            @Override
            public void run() {
                if(mWeakController.get() != null){
                    mWeakController.get().getTransportControls().skipToNext();
                }
                mWeakController.clear();
            }
        });
    }

    @Override
    public void postSkipPrevious(MediaControllerCompat controller) {
        final WeakReference<MediaControllerCompat> mWeakController = new WeakReference<>(controller);
        mBackGroundPool.execute(new Runnable() {
            @Override
            public void run() {
                if(mWeakController.get() != null){
                    mWeakController.get().getTransportControls().skipToPrevious();
                }
                mWeakController.clear();
            }
        });
    }

    @Override
    public void postSkipToPosition(MediaControllerCompat controller, final Long id) {
        final WeakReference<MediaControllerCompat> mWeakController = new WeakReference<>(controller);
        mBackGroundPool.execute(new Runnable() {
            @Override
            public void run() {
                if(mWeakController.get() != null){
                    mWeakController.get().getTransportControls().skipToQueueItem(id);
                }
                mWeakController.clear();
            }
        });
    }

    @Override
    public void shutdown() {
        if (mBackGroundPool != null) {
            mBackGroundPool.shutdown();
            mBackGroundPool = null;
        }
    }

}
