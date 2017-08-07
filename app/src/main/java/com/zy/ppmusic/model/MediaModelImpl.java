package com.zy.ppmusic.model;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.zy.ppmusic.contract.IMediaActivityContract;
import com.zy.ppmusic.utils.ScanMusicFile;

import java.lang.ref.WeakReference;
import java.util.Timer;

public class MediaModelImpl implements IMediaActivityContract.IModel {
    @Override
    public void refreshQueue(Context context,ScanMusicFile.OnScanComplete complete) {
        ScanMusicFile.getInstance().scanMusicFile(context).setOnScanComplete(complete);
    }
}
