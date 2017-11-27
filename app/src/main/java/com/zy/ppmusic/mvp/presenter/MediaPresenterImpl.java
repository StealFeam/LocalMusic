package com.zy.ppmusic.mvp.presenter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.zy.ppmusic.mvp.contract.IMediaActivityContract;
import com.zy.ppmusic.entity.MusicInfoEntity;
import com.zy.ppmusic.mvp.model.MediaModelImpl;
import com.zy.ppmusic.utils.DataTransform;
import com.zy.ppmusic.utils.FileUtils;
import com.zy.ppmusic.utils.ScanMusicFile;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MediaPresenterImpl implements IMediaActivityContract.IPresenter {
    private static final String TAG = "MediaPresenterImpl";
    private static final String CACHE_MODE_NAME = "CACHE_MODE";
    private static final String CACHE_MODE_KEY = "MODE_KEY";
    private WeakReference<IMediaActivityContract.IView> mViewWeak;
    private IMediaActivityContract.IModel mModel;
    private SharedPreferences mModeCachePreference;
    private boolean isScanning = false;

    public MediaPresenterImpl(IMediaActivityContract.IView mView) {
        this.mViewWeak = new WeakReference<>(mView);
        mModel = new MediaModelImpl();
    }

    @Override
    public void refreshQueue(Context context, boolean isRefresh) {
        mViewWeak.get().showLoading();
        if (isRefresh) {
            refresh(context, true);
        } else {
            Object localData = FileUtils.readObject(context.getCacheDir().getAbsolutePath());
            if (localData != null) {
                DataTransform.getInstance().transFormData((List<MusicInfoEntity>) localData);
                if (mViewWeak.get() != null) {
                    mViewWeak.get().loadFinished();
                    mViewWeak.get().hideLoading();
                }
            } else {
                refresh(context, false);
            }
        }
    }

    @Override
    public void changeMode(Context c,int mode) {
        if(mModeCachePreference == null){
            mModeCachePreference = c.getSharedPreferences(CACHE_MODE_NAME,Context.MODE_PRIVATE);
        }
        SharedPreferences.Editor edit = mModeCachePreference.edit();
        edit.putInt(CACHE_MODE_KEY,mode);
        edit.apply();
    }

    @Override
    public int getLocalMode(Context c) {
        if(mModeCachePreference == null){
            mModeCachePreference = c.getSharedPreferences(CACHE_MODE_NAME,Context.MODE_PRIVATE);
            if(!mModeCachePreference.contains(CACHE_MODE_KEY)){
                return 0;
            }
        }
        return mModeCachePreference.getInt(CACHE_MODE_KEY,0);
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
        mModel.refreshQueue(context, new ScanMusicFile.OnScanComplete() {
            @Override
            protected void onComplete(ArrayList<String> paths) {
                isScanning = false;
                Log.e(TAG, "onComplete: 扫描出来的" + paths.toString());
                DataTransform.getInstance().transFormData(context, paths);
                if (mViewWeak.get() != null) {
                    if (isRefresh) {
                        mViewWeak.get().refreshQueue(paths, true);
                    } else {
                        mViewWeak.get().loadFinished();
                    }
                    mViewWeak.get().hideLoading();
                }
            }
        });
    }

    @Override
    public void destroyView() {
        mViewWeak.clear();
        mViewWeak = null;
        mModel = null;
    }
}
