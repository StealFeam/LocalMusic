package com.zy.ppmusic.mvp.presenter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.zy.ppmusic.entity.MusicInfoEntity;
import com.zy.ppmusic.mvp.contract.IMediaActivityContract;
import com.zy.ppmusic.mvp.model.MediaMediaActivityModelImpl;
import com.zy.ppmusic.utils.DataTransform;
import com.zy.ppmusic.utils.FileUtils;
import com.zy.ppmusic.utils.ScanMusicFile;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * @author ZhiTouPC
 */
public class MediaPresenterImpl implements IMediaActivityContract.IMediaActivityPresenter {
    private static final String TAG = "MediaPresenterImpl";
    private static final String CACHE_MODE_NAME = "CACHE_MODE";
    private static final String CACHE_MODE_KEY = "MODE_KEY";
    private IMediaActivityContract.IMediaActivityModel mModel;
    private WeakReference<IMediaActivityContract.IMediaActivityView> mViewWeak;
    private SharedPreferences mModeCachePreference;
    private boolean isScanning = false;

    public MediaPresenterImpl(IMediaActivityContract.IMediaActivityView mView) {
        mViewWeak = new WeakReference<>(mView);
        mModel = new MediaMediaActivityModelImpl();
    }

    @Override
    public void refreshQueue(Context context, boolean isRefresh) {
        mViewWeak.get().showLoading();
        if (isRefresh) {
            refresh(context, true);
        } else {
            Object localData = FileUtils.readObject(context.getCacheDir().getAbsolutePath());
            if (localData != null) {
                TransFormTask.Builder builder = new TransFormTask.Builder();
                builder.listener = finishedListener;
                builder.isRefresh = false;
                builder.musicInfoEntities = (ArrayList<MusicInfoEntity>) localData;
                builder.context = context;
                builder.executeTask();
            } else {
                refresh(context, false);
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

    private void refresh(final Context context, final boolean isRefresh) {
        if (isScanning) {
            return;
        }
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

    private OnTaskFinishedListener finishedListener = new OnTaskFinishedListener() {
        @Override
        public void onRefreshQueue(ArrayList<String> paths) {
            if (mViewWeak.get() != null) {
                mViewWeak.get().refreshQueue(paths, true);
                mViewWeak.get().hideLoading();
            }
        }

        @Override
        public void loadFinished() {
            if (mViewWeak.get() != null) {
                mViewWeak.get().loadFinished();
                mViewWeak.get().hideLoading();
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


    private static class TransFormTask extends AsyncTask<Void, Void, ArrayList<String>> {
        private WeakReference<Context> mContextWeak;
        private ArrayList<String> mPaths;
        private ArrayList<MusicInfoEntity> entities;
        private boolean isRefresh;
        private OnTaskFinishedListener listener;

        private TransFormTask(Builder builder) {
            this.isRefresh = builder.isRefresh;
            this.listener = builder.listener;
            this.mContextWeak = new WeakReference<>(builder.context);
            this.mPaths = builder.mPaths;
            this.entities = builder.musicInfoEntities;
        }

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            if(mPaths == null){
                DataTransform.getInstance().transFormData(entities);
            }else{
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

            public Builder setContextWeak(Context context) {
                this.context = context;
                return this;
            }

            public Builder setPaths(ArrayList<String> mPaths) {
                this.mPaths = mPaths;
                return this;
            }

            public Builder setRefresh(boolean refresh) {
                isRefresh = refresh;
                return this;
            }

            public Builder setMusicInfoEntities(ArrayList<MusicInfoEntity> musicInfoEntities) {
                this.musicInfoEntities = musicInfoEntities;
                return this;
            }

            public Builder setListener(OnTaskFinishedListener listener) {
                this.listener = listener;
                return this;
            }

            private void executeTask() {
                TransFormTask task = new TransFormTask(this);
                task.execute();
            }
        }
    }

    @Override
    public void destroyView() {
        mViewWeak.clear();
        mViewWeak = null;
        mModel = null;
    }
}
