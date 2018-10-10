package com.zy.ppmusic.mvp.contract

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.util.SimpleArrayMap
import android.util.ArrayMap
import com.zy.ppmusic.mvp.base.AbstractBasePresenter
import com.zy.ppmusic.mvp.base.IBaseModel
import com.zy.ppmusic.mvp.base.IBaseView
import com.zy.ppmusic.utils.ScanMusicFile
import java.util.*

/**
 * @author ZhiTouPC
 */
interface IMediaActivityContract {
    interface IMediaActivityView : IBaseView {
        /**
         * 加载完成
         */
        fun loadFinished()

        /**
         * 刷新列表
         * @param isRefresh 是否是刷新
         */
        fun refreshQueue(isRefresh: Boolean)

        fun setRepeatMode(mode:Int)

        fun setDeleteResult(isSuccess:Boolean,path:String?)

        /**
         * 检查服务连接
         */
        fun checkConnection()

        /**
         * 显示LoadingDialog
         */
        fun showLoading()

        /**
         * 隐藏LoadingDialog
         */
        fun hideLoading()
    }

    abstract class AbstractMediaActivityPresenter(iMediaActivityView: IMediaActivityView) :
            AbstractBasePresenter<IMediaActivityView, IMediaActivityModel>(iMediaActivityView) {
        abstract fun attachModelController(controller: MediaControllerCompat?)

        abstract fun getGrantedRootUri():String

        abstract fun getChildrenUri():String

        abstract fun setGrantedRootUri(uri:String,child:String)

        abstract fun removeQueueItem(position:Int)

        /**
         * 刷新列表
         *
         * @param context   环境
         * @param isRefresh 是否是刷新
         */
        abstract fun refreshQueue(context: Context, isRefresh: Boolean)

        /**
         * 获取本地模式数据
         *
         * @param c 环境
         * @return 本地数据
         */
        abstract fun getLocalMode(c: Context)

        /**
         * 删除文件
         *
         * @param path 本地文件路径
         * @return 删除结果
         */
        abstract fun deleteFile(path: String?)

        /**
         * 向服务发送命令
         * @param method 调用的方法
         * @param params 参数
         * @param resultReceiver 接受回调的对象
         */
        abstract fun sendCommand(method: String, params: Bundle, resultReceiver: ResultReceiver)

        /**
         * 播放指定的媒体
         * @param mediaId 媒体的id
         * @param extra 额外的参数
         */
        abstract fun playWithId(mediaId: String, extra: Bundle)


        /**
         * 发送自定义消息
         * @param action 自定义消息标志
         * @param extra 自定义消息的参数
         */
        abstract fun sendCustomAction(action: String, extra: Bundle)

        /**
         * 设置列表播放模式
         * @param context 用于本地存储所需
         * @param mode 播放模式
         */
        abstract fun setRepeatMode(context: Context, mode: Int)

        /**
         * 下一首
         */
        abstract fun skipNext()

        /**
         * 播放指定位置的媒体
         * @param id 媒体在列表中的位置
         */
        abstract fun skipToPosition(id: Long)

        /**
         * 上一首
         */
        abstract fun skipPrevious()

    }

    interface IMediaActivityModel : IBaseModel {

        fun getGrantedRootUri():String

        fun getGrantedChildrenUri():String

        fun saveGrantedUri(root:String,child:String)

        fun attachController(controller: MediaControllerCompat?)

        fun getLocalMode(c:Context,e:(Int) -> Unit)

        fun removeQueueItem(position:Int)

        /**
         * 刷新列表
         *
         * @param context 环境
         * @param l       监听回调
         */
        fun refreshQueue(context: Context, l: ScanMusicFile.AbstractOnScanComplete)

        /**
         * 加载本地数据
         *
         * @param path     本地数据路径
         * @param callback 加载完成回调
         */
        fun loadLocalData(path: String, callback: IOnLocalDataLoadFinished)

        /**
         * 向服务发送命令
         * @param method 调用的方法
         * @param params 参数
         * @param resultReceiver 接受回调的对象
         */
        fun postSendCommand(method: String, params: Bundle, resultReceiver: ResultReceiver)

        /**
         * 播放指定的媒体
         * @param mediaId 媒体的id
         * @param extra 额外的参数
         */
        fun postPlayWithId(mediaId: String, extra: Bundle)

        /**
         * 发送自定义消息
         * @param action 自定义消息标志
         * @param extra 自定义消息的参数
         */
        fun postSendCustomAction(action: String, extra: Bundle)

        /**
         * 设置列表播放模式
         * @param c 上下文
         * @param mode 播放模式
         */
        fun postSetRepeatMode(c:Context,mode: Int)

        /**
         * 下一首
         */
        fun postSkipNext()

        /**
         * 上一首
         */
        fun postSkipPrevious()

        /**
         * 播放指定位置的媒体
         * @param id 媒体在列表中的位置
         */
        fun postSkipToPosition(id: Long)

        /**
         * 关闭线程池
         */
        fun shutdown()

        interface IOnLocalDataLoadFinished {
            /**
             * 加载完成回调
             * @param data 本地的数据
             */
            fun callBack(data: Any?)
        }
    }
}
