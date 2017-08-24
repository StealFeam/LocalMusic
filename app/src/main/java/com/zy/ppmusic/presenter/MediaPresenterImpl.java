package com.zy.ppmusic.presenter;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;

import com.zy.ppmusic.contract.IMediaActivityContract;
import com.zy.ppmusic.entity.MusicInfoEntity;
import com.zy.ppmusic.model.MediaModelImpl;
import com.zy.ppmusic.utils.DataTransform;
import com.zy.ppmusic.utils.FileUtils;
import com.zy.ppmusic.utils.ScanMusicFile;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MediaPresenterImpl implements IMediaActivityContract.IPresenter {
    private static final String TAG = "MediaPresenterImpl";
    private WeakReference<IMediaActivityContract.IView> mViewWeak;
    private IMediaActivityContract.IModel mModel;

    public MediaPresenterImpl(IMediaActivityContract.IView mView) {
        this.mViewWeak = new WeakReference<>(mView);
        mModel = new MediaModelImpl();
    }

    @Override
    public void refreshQueue(Context context,boolean isRefresh) {
        mViewWeak.get().showLoading();
        if(isRefresh){
            refresh(context,true);
        }else{
            Object localData = FileUtils.readObject(context.getCacheDir().getAbsolutePath());
            if (localData != null) {
                DataTransform.getInstance().transFormData((List<MusicInfoEntity>) localData);
                if (mViewWeak.get() != null) {
                    mViewWeak.get().loadFinished();
                    mViewWeak.get().hideLoading();
                }
            } else {
                refresh(context,false);
            }
        }
    }

    private void refresh(final Context context,final boolean isRefresh){
        mModel.refreshQueue(context, new ScanMusicFile.OnScanComplete() {
            @Override
            protected void onComplete(ArrayList<String> paths) {
                Log.e(TAG, "onComplete: 扫描出来的"+paths.toString());
                DataTransform.getInstance().transFormData(context, paths);
                if (mViewWeak.get() != null) {
                    if(isRefresh){
                        mViewWeak.get().refreshQueue(paths,true);
                    }else{
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
