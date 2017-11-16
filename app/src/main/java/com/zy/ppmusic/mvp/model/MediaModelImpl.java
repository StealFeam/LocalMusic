package com.zy.ppmusic.mvp.model;

import android.content.Context;

import com.zy.ppmusic.mvp.contract.IMediaActivityContract;
import com.zy.ppmusic.utils.ScanMusicFile;

public class MediaModelImpl implements IMediaActivityContract.IModel {

    @Override
    public void refreshQueue(Context context, ScanMusicFile.OnScanComplete complete) {
        ScanMusicFile.getInstance().setOnScanComplete(complete).scanMusicFile(context);
    }

}
