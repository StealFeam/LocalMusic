package com.zy.ppmusic.model;

import android.content.Context;

import com.zy.ppmusic.contract.IMediaActivityContract;
import com.zy.ppmusic.utils.ScanMusicFile;

public class MediaModelImpl implements IMediaActivityContract.IModel {

    @Override
    public void refreshQueue(Context context, ScanMusicFile.OnScanComplete complete) {
        ScanMusicFile.getInstance().setOnScanComplete(complete).scanMusicFile(context);
    }

}
