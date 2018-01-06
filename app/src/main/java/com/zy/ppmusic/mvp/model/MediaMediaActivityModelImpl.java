package com.zy.ppmusic.mvp.model;

import android.content.Context;

import com.zy.ppmusic.mvp.contract.IMediaActivityContract;
import com.zy.ppmusic.utils.ScanMusicFile;

/**
 * @author ZhiTouPC
 */
public class MediaMediaActivityModelImpl implements IMediaActivityContract.IMediaActivityModel {

    @Override
    public void refreshQueue(Context context, ScanMusicFile.AbstractOnScanComplete complete) {
        ScanMusicFile.getInstance().setOnScanComplete(complete).scanMusicFile(context);
    }

}
