package com.zy.ppmusic.utils;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import org.jetbrains.annotations.Contract;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kotlin.coroutines.experimental.CoroutineContext;

/**
 * 扫描本地音乐文件
 */
public class ScanMusicFile {
    private static final String TAG = "ScanMusicFile";
    private static final int COUNT_CHANGE = 0X001;//扫描的数量发生了变化
    private static final int SCAN_COMPLETE = 0X000;//扫描完成

    private String[] mSupportMedia = {".mp3", ".wav", ".wma"};//所支持的音乐格式
    private ExecutorService executor = Executors.newSingleThreadExecutor();//单一线程池
    private OnScanComplete onScanComplete;//扫描完成回调
    private List<String> mPathList = new ArrayList<>();//扫描到的音乐路径集合
    private ScanHandler mHandler = new ScanHandler(this);//线程回调的handler
    private NumberFormat format = NumberFormat.getInstance(Locale.CHINA);//数字格式化工具
    private String mInnerStoragePath;//内部存储路径
    private String mExternalStoragePath;//外部存储路径

    private static class ScanInstance {
        private static ScanMusicFile scanMusicFile = new ScanMusicFile();
    }

    private ScanMusicFile() {
        format.setMaximumIntegerDigits(2);
        format.setMaximumFractionDigits(2);
    }

    @Contract(pure = true)
    public static ScanMusicFile getInstance() {
        return ScanInstance.scanMusicFile;
    }

    public ScanMusicFile setOnScanComplete(OnScanComplete complete) {
        this.onScanComplete = complete;
        return this;
    }

    public void scanMusicFile(Context context) {
        mInnerStoragePath = FileUtils.getStoragePath(context,false);
        mExternalStoragePath = FileUtils.getStoragePath(context,true);
        executor.submit(new Runnable() {
            @Override
            public void run() {
                searchFile(new File(mInnerStoragePath));
                searchFile(new File(mExternalStoragePath));

                for (int i = 0; i < mPathList.size(); i++) {
                    File file = new File(mPathList.get(i));
                    double size = ((double) file.length() / 1024d) / 1024d;
                    if (size > 1d) {
                        Log.w(TAG, file.getAbsolutePath() + "," + format.format(size) + "M");
                    } else {
                        mPathList.remove(i);
                    }
                }
                mHandler.sendEmptyMessage(0);
            }
        });
    }

    private static class ScanHandler extends Handler {
        private WeakReference<ScanMusicFile> weak;

        private ScanHandler(ScanMusicFile scanMusicFile) {
            this.weak = new WeakReference<ScanMusicFile>(scanMusicFile);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (weak.get() != null) {
                ScanMusicFile scanMusicFile = weak.get();
                switch (msg.what) {
                    case SCAN_COMPLETE:
                        if (scanMusicFile.onScanComplete != null) {
                            scanMusicFile.onScanComplete.onComplete(scanMusicFile.mPathList);
                        }
                        break;
                    case COUNT_CHANGE:
                        if (scanMusicFile.onScanComplete != null) {
                            scanMusicFile.onScanComplete.onCountChange(scanMusicFile.mPathList.size());
                        }
                        break;
                    default:
                        break;
                }


            }
        }
    }

    /**
     * 遍历文件目录下的所有文件
     *
     * @param file 需要扫描的文件目录
     */
    private void searchFile(File file) {
        if (file.isDirectory()) {
            File[] items = file.listFiles();
            for (File item : items) {
                searchFile(item);
            }
            return;
        }
        //过滤没有后缀名的文件
        if (!file.getName().contains(".")) {
            return;
        }
        //判断文件的类型是否支持
        for (String format : mSupportMedia) {
            if (file.getName().endsWith(format)) {
//                Log.e(TAG, "searchFile: "+file.getAbsolutePath());
                mPathList.add(file.getAbsolutePath());
                mHandler.sendEmptyMessage(COUNT_CHANGE);
                return;
            }
        }


    }

    public abstract class OnScanComplete {
        private OnScanComplete(){

        }
        abstract void onComplete(List<String> paths);
        void onCountChange(int size){

        }
    }
}
