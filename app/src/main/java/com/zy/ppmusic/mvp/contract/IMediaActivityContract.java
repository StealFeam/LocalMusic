package com.zy.ppmusic.mvp.contract;

import android.content.Context;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.session.MediaControllerCompat;

import com.zy.ppmusic.mvp.base.AbstractBasePresenter;
import com.zy.ppmusic.mvp.base.IBaseModel;
import com.zy.ppmusic.mvp.base.IBaseView;
import com.zy.ppmusic.utils.ScanMusicFile;

import java.util.ArrayList;

/**
 * @author ZhiTouPC
 */
public interface IMediaActivityContract {
    interface IMediaActivityView extends IBaseView {
        /**
         * 加载完成
         */
        void loadFinished();

        /**
         * 刷新列表
         *
         * @param mPathList 扫描到的集合
         * @param isRefresh 是否是刷新
         */
        void refreshQueue(ArrayList<String> mPathList, boolean isRefresh);

        /**
         * 显示LoadingDialog
         */
        void showLoading();

        /**
         * 隐藏LoadingDialog
         */
        void hideLoading();
    }

    abstract class AbstractMediaActivityPresenter extends
            AbstractBasePresenter<IMediaActivityView,IMediaActivityModel> {

        public AbstractMediaActivityPresenter(IMediaActivityView iMediaActivityView) {
            super(iMediaActivityView);
        }

        /**
         * 刷新列表
         *
         * @param context   环境
         * @param isRefresh 是否是刷新
         */
        public abstract void refreshQueue(Context context, boolean isRefresh);

        /**
         * 修改本地播放模式
         *
         * @param c    环境
         * @param mode 模式对应码
         */
        public abstract void changeMode(Context c, int mode);

        /**
         * 获取本地模式数据
         *
         * @param c 环境
         * @return 本地数据
         */
        public abstract int getLocalMode(Context c);

        /**
         * 删除文件
         *
         * @param path 本地文件路径
         * @return 删除结果
         */
        public abstract boolean deleteFile(String path);

        /**
         * 向服务发送命令
         * @param controller 服务连接
         * @param method 调用的方法
         * @param params 参数
         * @param resultReceiver 接受回调的对象
         */
        public abstract void sendCommand(MediaControllerCompat controller, String method, Bundle params,
                                         ResultReceiver resultReceiver);

        /**
         * 播放指定的媒体
         * @param controller 服务连接
         * @param mediaId 媒体的id
         * @param extra 额外的参数
         */
        public abstract void playWithId(MediaControllerCompat controller, String mediaId, Bundle extra);


        /**
         * 发送自定义消息
         * @param controller 服务连接
         * @param action 自定义消息标志
         * @param extra 自定义消息的参数
         */
        public abstract void sendCustomAction(MediaControllerCompat controller, String action, Bundle extra);

        /**
         * 设置列表播放模式
         * @param context 用于本地存储所需
         * @param controller 服务连接
         * @param mode 播放模式
         */
        public abstract void setRepeatMode(Context context, MediaControllerCompat controller, int mode);

        /**
         * 下一首
         * @param controller 服务连接
         */
        public abstract void skipNext(MediaControllerCompat controller);

        /**
         * 播放指定位置的媒体
         * @param controller 服务连接
         * @param id 媒体在列表中的位置
         */
        public abstract void skipToPosition(MediaControllerCompat controller, Long id);

        /**
         * 上一首
         * @param controller 服务连接
         */
        public abstract void skipPrevious(MediaControllerCompat controller);
    }

    interface IMediaActivityModel extends IBaseModel {
        /**
         * 刷新列表
         *
         * @param context 环境
         * @param l       监听回调
         */
        void refreshQueue(Context context, ScanMusicFile.AbstractOnScanComplete l);

        /**
         * 加载本地数据
         *
         * @param path     本地数据路径
         * @param callback 加载完成回调
         */
        void loadLocalData(String path, IOnLocalDataLoadFinished callback);

        /**
         * 向服务发送命令
         * @param controller 服务连接
         * @param method 调用的方法
         * @param params 参数
         * @param resultReceiver 接受回调的对象
         */
        void postSendCommand(MediaControllerCompat controller,String method, Bundle params, ResultReceiver resultReceiver);

        /**
         * 播放指定的媒体
         * @param controller 服务连接
         * @param mediaId 媒体的id
         * @param extra 额外的参数
         */
        void postPlayWithId(MediaControllerCompat controller,String mediaId,Bundle extra);

        /**
         * 发送自定义消息
         * @param controller 服务连接
         * @param action 自定义消息标志
         * @param extra 自定义消息的参数
         */
        void postSendCustomAction(MediaControllerCompat controller,String action,Bundle extra);

        /**
         * 设置列表播放模式
         * @param controller 服务连接
         * @param mode 播放模式
         */
        void postSetRepeatMode(MediaControllerCompat controller,int mode);

        /**
         * 下一首
         * @param controller 服务连接
         */
        void postSkipNext(MediaControllerCompat controller);

        /**
         * 上一首
         * @param controller 服务连接
         */
        void postSkipPrevious(MediaControllerCompat controller);

        /**
         * 播放指定位置的媒体
         * @param controller 服务连接
         * @param id 媒体在列表中的位置
         */
        void postSkipToPosition(MediaControllerCompat controller,Long id);

        /**
         * 关闭线程池
         */
        void shutdown();

        interface IOnLocalDataLoadFinished {
            /**
             * 加载完成回调
             * @param data 本地的数据
             */
            void callBack(Object data);
        }
    }
}
