package com.zy.ppmusic.mvp.presenter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;

import com.zy.ppmusic.entity.MusicInfoEntity;
import com.zy.ppmusic.mvp.contract.IMediaActivityContract;
import com.zy.ppmusic.mvp.model.MediaActivityModelImpl;
import com.zy.ppmusic.utils.DataTransform;
import com.zy.ppmusic.utils.FileUtils;
import com.zy.ppmusic.utils.PrintOut;
import com.zy.ppmusic.utils.ScanMusicFile;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * @author ZhiTouPC
 */
public class MediaPresenterImpl extends IMediaActivityContract.AbstractMediaActivityPresenter {
    private static final String TAG = "MediaPresenterImpl";
    private static final String CACHE_MODE_NAME = "CACHE_MODE";
    private static final String CACHE_MODE_KEY = "MODE_KEY";
    private SharedPreferences mModeCachePreference;
    private boolean isScanning = false;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public MediaPresenterImpl(IMediaActivityContract.IMediaActivityView mView) {
        super(mView);
    }

    @Override
    protected IMediaActivityContract.IMediaActivityModel createModel() {
        return new MediaActivityModelImpl();
    }

    @Override
    public void refreshQueue(final Context context, boolean isRefresh) {
        mView.get().showLoading();
        //重新扫描本地文件或者初次扫描
        if (isRefresh) {
            refresh(context, true);
        } else {
            //内存有数据
            if (DataTransform.getInstance().getMediaItemList().size() > 0) {
                if (mView.get() != null) {
                    mView.get().loadFinished();
                    mView.get().hideLoading();
                }
            } else {
                PrintOut.i("开始读取本地数据");
                mModel.loadLocalData(context.getCacheDir().getAbsolutePath(), new IMediaActivityContract
                        .IMediaActivityModel.IOnLocalDataLoadFinished() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public void callBack(Object data) {
                        if (data != null) {
                            PrintOut.i("读取到本地缓存数据");
                            TransFormTask.Builder builder = new TransFormTask.Builder();
                            builder.listener = finishedListener;
                            builder.isRefresh = false;
                            builder.musicInfoEntities = (ArrayList<MusicInfoEntity>) data;
                            builder.context = context;
                            builder.executeTask();
                        } else {
                            //回到主线程
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    PrintOut.i("未读取到本地数据");
                                    refresh(context, false);
                                }
                            });
                        }
                    }
                });
            }

        }
    }

    @Override
    public void changeMode(Context c, int mode) {
        if (mModeCachePreference == null) {
            mModeCachePreference = c.getSharedPreferences(CACHE_MODE_NAME, Context.MODE_PRIVATE);
        }
        SharedPreferences.Editor edit = mModeCachePreference.edit();
        edit.putInt(CACHE_MODE_KEY, mode);
        edit.apply();
    }

    @Override
    public int getLocalMode(Context c) {
        if (mModeCachePreference == null) {
            mModeCachePreference = c.getSharedPreferences(CACHE_MODE_NAME, Context.MODE_PRIVATE);
            if (!mModeCachePreference.contains(CACHE_MODE_KEY)) {
                return 0;
            }
        }
        return mModeCachePreference.getInt(CACHE_MODE_KEY, 0);
    }

    @Override
    public boolean deleteFile(String path) {
        return FileUtils.deleteFile(path);
    }

    @Override
    public void sendCommand(MediaControllerCompat controller, String method, Bundle params, ResultReceiver resultReceiver) {
        mModel.postSendCommand(controller, method, params, resultReceiver);
    }

    @Override
    public void playWithId(MediaControllerCompat controller, String mediaId, Bundle extra) {
        mModel.postPlayWithId(controller, mediaId, extra);
    }

    @Override
    public void sendCustomAction(MediaControllerCompat controller, String action, Bundle extra) {
        mModel.postSendCustomAction(controller, action, extra);
    }

    @Override
    public void setRepeatMode(Context context, MediaControllerCompat controller, int mode) {
        mModel.postSetRepeatMode(controller, mode);
        changeMode(context, mode);
    }

    @Override
    public void skipNext(MediaControllerCompat controller) {
        mModel.postSkipNext(controller);
    }

    @Override
    public void skipToPosition(MediaControllerCompat controller, Long id) {
        mModel.postSkipToPosition(controller, id);
    }

    @Override
    public void skipPrevious(MediaControllerCompat controller) {
        mModel.postSkipPrevious(controller);
    }

    private void refresh(final Context context, final boolean isRefresh) {
        if (isScanning) {
            return;
        }
        PrintOut.i("开始扫描本地媒体。。。。。。");
        isScanning = true;
        mModel.refreshQueue(context, new ScanMusicFile.AbstractOnScanComplete() {
            @Override
            protected void onComplete(ArrayList<String> paths) {
                isScanning = false;
                Log.e(TAG, "onComplete: 扫描出来的" + paths.toString());
                TransFormTask.Builder builder = new TransFormTask.Builder();
                builder.context = context;
                builder.isRefresh = isRefresh;
                builder.mPaths = paths;
                builder.listener = finishedListener;
                builder.executeTask();
            }
        });
    }

    private final OnTaskFinishedListener finishedListener = new OnTaskFinishedListener() {
        @Override
        public void onRefreshQueue(ArrayList<String> paths) {
            if (mView.get() != null) {
                mView.get().refreshQueue(paths, true);
                mView.get().hideLoading();
            }
        }

        @Override
        public void loadFinished() {
            if (mView.get() != null) {
                mView.get().loadFinished();
                mView.get().hideLoading();
            }
        }
    };

    private interface OnTaskFinishedListener {

        /**
         * 完成时回调
         *
         * @param paths 路径集合
         */
        void onRefreshQueue(ArrayList<String> paths);

        /**
         * 加载完成
         */
        void loadFinished();
    }

    /**
     * 负责将本地数据或者扫描到的数据数据转换
     */
    private static class TransFormTask extends AsyncTask<String, Void, ArrayList<String>> {
        private final WeakReference<Context> mContextWeak;
        private final ArrayList<String> mPaths;
        private final ArrayList<MusicInfoEntity> entities;
        private final boolean isRefresh;
        private final OnTaskFinishedListener listener;

        private TransFormTask(Builder builder) {
            this.isRefresh = builder.isRefresh;
            this.listener = builder.listener;
            this.mContextWeak = new WeakReference<>(builder.context);
            this.mPaths = builder.mPaths;
            this.entities = builder.musicInfoEntities;
        }

        @Override
        protected ArrayList<String> doInBackground(String... paths) {
            if (mPaths == null) {
                DataTransform.getInstance().transFormData(entities);
            } else {
                DataTransform.getInstance().transFormData(mContextWeak.get(), mPaths);
            }
            return mPaths;
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            super.onPostExecute(strings);
            if (listener != null) {
                if (isRefresh) {
                    listener.onRefreshQueue(strings);
                } else {
                    listener.loadFinished();
                }
            }
        }

        private static class Builder {
            private Context context;
            private ArrayList<String> mPaths;
            private boolean isRefresh;
            private ArrayList<MusicInfoEntity> musicInfoEntities;
            private OnTaskFinishedListener listener;

            private void executeTask() {
                TransFormTask task = new TransFormTask(this);
                task.execute();
            }
        }
    }

    @Override
    public void detachViewAndModel() {
        super.detachViewAndModel();
        mModel.shutdown();
    }

}
