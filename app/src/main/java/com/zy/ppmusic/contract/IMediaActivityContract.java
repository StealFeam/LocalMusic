package com.zy.ppmusic.contract;

import android.content.Context;

import com.zy.ppmusic.base.IBaseModel;
import com.zy.ppmusic.base.IBasePresenter;
import com.zy.ppmusic.base.IBaseView;
import com.zy.ppmusic.utils.ScanMusicFile;

import java.util.ArrayList;

public interface IMediaActivityContract {
    interface IView extends IBaseView {
        void refreshQueue(ArrayList<String> mPathList);
        void showLoading();
        void hideLoading();
    }

    interface IPresenter extends IBasePresenter {
        void refreshQueue(Context context);
    }

    interface IModel extends IBaseModel {
        void refreshQueue(Context context,ScanMusicFile.OnScanComplete onScanComplete);
    }
}
