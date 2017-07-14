package com.zy.ppmusic.presenter;

import com.zy.ppmusic.contract.IMediaActivityContract;
import com.zy.ppmusic.model.MediaModelImpl;

public class MediaPresenterImpl implements IMediaActivityContract.IPresenter {
    private IMediaActivityContract.IView mView;
    private IMediaActivityContract.IModel mModel;

    public MediaPresenterImpl(IMediaActivityContract.IView mView) {
        this.mView = mView;
        mModel = new MediaModelImpl();
    }

    @Override
    public void refreshQueue() {
        mView.refreshQueue(mModel.refreshQueue());
    }
}
