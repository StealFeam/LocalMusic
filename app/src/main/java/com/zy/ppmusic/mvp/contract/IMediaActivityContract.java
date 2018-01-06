package com.zy.ppmusic.mvp.contract;

import android.content.Context;

import com.zy.ppmusic.base.IBaseModel;
import com.zy.ppmusic.base.IBasePresenter;
import com.zy.ppmusic.base.IBaseView;
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

    interface IMediaActivityPresenter extends IBasePresenter {

        /**
         * 刷新列表
         *
         * @param context   环境
         * @param isRefresh 是否是刷新
         */
        void refreshQueue(Context context, boolean isRefresh);

        /**
         * 修改本地播放模式
         *
         * @param c    环境
         * @param mode 模式对应码
         */
        void changeMode(Context c, int mode);

        /**
         * 获取本地模式数据
         *
         * @param c 环境
         * @return 本地数据
         */
         int getLocalMode(Context c);

        /**
         * 删除文件
         *
         * @param path 本地文件路径
         * @return 删除结果
         */
        boolean deleteFile(String path);
    }

    interface IMediaActivityModel extends IBaseModel {
        /**
         * 刷新列表
         *
         * @param context 环境
         * @param l       监听回调
         */
        void refreshQueue(Context context, ScanMusicFile.AbstractOnScanComplete l);
    }
}
