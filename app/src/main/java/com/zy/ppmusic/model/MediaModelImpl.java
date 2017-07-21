package com.zy.ppmusic.model;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import com.zy.ppmusic.contract.IMediaActivityContract;
import com.zy.ppmusic.utils.ScanMusicFile;

import java.util.ArrayList;

public class MediaModelImpl implements IMediaActivityContract.IModel {

    @Override
    public void refreshQueue(Context context,ScanMusicFile.OnScanComplete complete) {
        ScanMusicFile.getInstance().scanMusicFile(context).setOnScanComplete(complete);
    }
}
