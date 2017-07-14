package com.zy.ppmusic.contract;

import com.zy.ppmusic.base.IBaseModel;
import com.zy.ppmusic.base.IBasePresenter;
import com.zy.ppmusic.base.IBaseView;

import java.util.ArrayList;

public interface IMediaActivityContract {
    interface IView extends IBaseView {
        void refreshQueue(ArrayList<String> mPathList);
    }

    interface IPresenter extends IBasePresenter {
        void refreshQueue();
    }

    interface IModel extends IBaseModel {
        ArrayList<String> refreshQueue();
    }
}
