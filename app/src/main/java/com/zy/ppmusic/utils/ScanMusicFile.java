package com.zy.ppmusic.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * 扫描本地音乐文件
 *
 * @author lengs
 */
public class ScanMusicFile {
    private static final String TAG = "ScanMusicFile";
    /**
     * 扫描的数量发生了变化
     */
    private static final int COUNT_CHANGE = 0X001;
    /**
     * 扫描完成
     */
    private static final int SCAN_COMPLETE = 0X000;

    private volatile ArrayList<AbstractOnScanComplete> callBackList = new ArrayList<>();
    /**
     * 扫描到的音乐路径集合
     */
    private volatile ArrayList<String> mPathList = new ArrayList<>();
    /**
     * 内部存储路径
     */
    private String mInnerStoragePath;
    /**
     * 外部存储路径
     */
    private String mExternalStoragePath;
    /**
     * 线程回调的handler
     */
    private ScanHandler mHandler;
    /**
     * 扫描任务
     */
    private Runnable mScanTask;

    private static class ScanInstance {
        private static ScanMusicFile instance = new ScanMusicFile();
    }

    private ScanMusicFile() {
    }

    public static ScanMusicFile get() {
        return ScanInstance.instance;
    }

    public ScanMusicFile setOnScanComplete(AbstractOnScanComplete complete) {
        if (!callBackList.contains(complete)) {
            callBackList.add(complete);
        }
        return this;
    }


    public void scanMusicFile(final Context c) {
        final Context context = c.getApplicationContext();
        if (mInnerStoragePath == null) {
            mInnerStoragePath = FileUtils.INSTANCE.getStoragePath(context, false);
            mExternalStoragePath = FileUtils.INSTANCE.getStoragePath(context, true);
        }
        if (mScanTask == null) {
            mScanTask = new Runnable() {
                @Override
                public void run() {
                    if (mPathList != null && mPathList.size() > 0) {
                        mPathList.clear();
                    }
                    Log.d(TAG, "run: 扫描开始");
                    if (mInnerStoragePath != null) {
                        searchFile(new File(mInnerStoragePath));
                        Log.e(TAG, "run: 扫描内部存储结束");
                    }

                    if (mExternalStoragePath != null) {
                        searchFile(new File(mExternalStoragePath));
                        Log.e(TAG, "run: 扫描外部存储结束");
                    }
                    Log.d(TAG, "run: 扫描结束");
                    mHandler.sendEmptyMessage(SCAN_COMPLETE);
                }
            };
            mHandler = new ScanHandler(this);
        }
        TaskPool.INSTANCE.execute(mScanTask);
    }

    private static class ScanHandler extends Handler {
        private WeakReference<ScanMusicFile> weak;

        private ScanHandler(ScanMusicFile scanMusicFile) {
            super(Looper.getMainLooper());
            this.weak = new WeakReference<>(scanMusicFile);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (weak.get() != null) {
                ScanMusicFile scanMusicFile = weak.get();
                switch (msg.what) {
                    case SCAN_COMPLETE:
                        if (scanMusicFile.callBackList.size() > 0) {
                            for (int i = scanMusicFile.callBackList.size() - 1; i >= 0; i--) {
                                AbstractOnScanComplete callback = scanMusicFile.callBackList.get(i);
                                callback.onComplete(scanMusicFile.mPathList);
                                scanMusicFile.callBackList.remove(callback);
                            }
                        }
                        break;
                    case COUNT_CHANGE:
                        if (scanMusicFile.callBackList.size() > 0) {
                            for (AbstractOnScanComplete callback : scanMusicFile.callBackList) {
                                callback.onCountChange(scanMusicFile.mPathList.size());
                            }
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
        if (file.isDirectory() && file.listFiles() != null) {
            File[] items = file.listFiles();
            for (File item : items) {
                searchFile(item);
            }
            return;
        }
        String dot = ".";
        //过滤没有后缀名的文件
        if (!file.getName().contains(dot)) {
            return;
        }
        int index = file.getName().indexOf(dot);
        int length = file.getName().length();
        //xxx.x以及xxx.xxxxx格式不支持
        if(index > (length -2) || index < (length - 4)){
            return;
        }
        //判断文件的类型是否支持
        for (String format : SupportMediaType.INSTANCE.getSUPPORT_TYPE()) {
            if (file.getName().endsWith(format)) {
                // * 1L  1M的大小
                long size = 1024L * 1024L;
                if (size < file.length()) {
                    Log.w(TAG, file.getAbsolutePath() + ",length=" + file.length());
                    mPathList.add(file.getAbsolutePath());
                    mHandler.sendEmptyMessage(COUNT_CHANGE);
                }
                return;
            }
        }
    }

    public abstract static class AbstractOnScanComplete {
        /**
         * 扫描完成
         *
         * @param paths 路径集合
         */
        protected abstract void onComplete(ArrayList<String> paths);

        public void onCountChange(int size) {

        }
    }
}
