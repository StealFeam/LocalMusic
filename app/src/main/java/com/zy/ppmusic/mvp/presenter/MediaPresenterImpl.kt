package com.zy.ppmusic.mvp.presenter

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.zy.ppmusic.mvp.contract.IMediaActivityContract
import com.zy.ppmusic.mvp.model.MediaActivityModelImpl
import com.zy.ppmusic.utils.DataTransform
import com.zy.ppmusic.utils.FileUtils
import com.zy.ppmusic.utils.PrintLog
import com.zy.ppmusic.utils.ScanMusicFile

/**
 * @author ZhiTouPC
 */
class MediaPresenterImpl(view: IMediaActivityContract.IMediaActivityView) :
        IMediaActivityContract.AbstractMediaActivityPresenter(view) {
    private var mCachePreference: SharedPreferences? = null
    private var isScanning = false
    private val mMainHandler = Handler(Looper.getMainLooper())

    override fun getChildrenUri(): String {
        return mCachePreference?.getString(CACHE_CHILD_URI, "") ?: ""
    }

    override fun getGrantedRootUri(): String {
        return mCachePreference?.getString(CACHE_ROOT_URI, "") ?: ""
    }

    override fun setGrantedRootUri(uri: String, child: String) {
        mCachePreference?.edit()?.putString(CACHE_ROOT_URI, uri)?.putString(CACHE_CHILD_URI, child)?.apply()
    }

    override fun createModel(): IMediaActivityContract.IMediaActivityModel {
        return MediaActivityModelImpl()
    }

    override fun attachModelController(controller: MediaControllerCompat?) {
        mModel.attachController(controller)
    }

    override fun refreshQueue(context: Context, isRefresh: Boolean) {
        if (mView.get() == null) {
            PrintLog.i("view is null")
            return
        }
        mView.get()?.showLoading()
        //重新扫描本地文件或者初次扫描
        if (isRefresh) {
            refresh(context)
        } else {
            //内存有数据
            if (DataTransform.get().mediaItemList.size > 0) {
                mView.get()?.loadFinished()
                mView.get()?.hideLoading()
            } else {
                PrintLog.i("开始读取本地数据")
                mModel.loadLocalData(context.cacheDir.absolutePath,
                        object : IMediaActivityContract.IMediaActivityModel.IOnLocalDataLoadFinished {
                            override fun callBack(data: Any?) {
                                //回到主线程
                                mMainHandler.post {
                                    data?.apply {
                                        PrintLog.i("读取到本地缓存数据")
                                        mView?.get()?.loadFinished()
                                        mView?.get()?.hideLoading()
                                    } ?: let {
                                        PrintLog.i("未读取到本地数据")
                                        refresh(context)
                                    }
                                }
                            }
                        })
            }

        }
    }

    private fun initCachePreference(context: Context) {
        if (mCachePreference == null) {
            mCachePreference = context.getSharedPreferences(CACHE_MODE_NAME, Context.MODE_PRIVATE)
        }
    }

    override fun changeMode(c: Context, mode: Int) {
        initCachePreference(c)
        if (mCachePreference!!.getInt(CACHE_MODE_KEY, PlaybackStateCompat.REPEAT_MODE_NONE) == mode) {
            return
        }
        val edit = mCachePreference!!.edit()
        edit.putInt(CACHE_MODE_KEY, mode)
        edit.apply()
    }

    override fun getLocalMode(c: Context): Int {
        initCachePreference(c)
        return mCachePreference?.getInt(CACHE_MODE_KEY, PlaybackStateCompat.REPEAT_MODE_NONE)
                ?: PlaybackStateCompat.REPEAT_MODE_NONE
    }

    override fun deleteFile(path: String?): Boolean {
        return FileUtils.deleteFile(path)
    }

    override fun sendCommand(method: String, params: Bundle, resultReceiver: ResultReceiver) {
        mModel.postSendCommand(method, params, resultReceiver)
    }

    override fun playWithId(mediaId: String, extra: Bundle) {
        mModel.postPlayWithId(mediaId, extra)
    }

    override fun sendCustomAction(action: String, extra: Bundle) {
        mModel.postSendCustomAction(action, extra)
    }

    override fun setRepeatMode(context: Context, mode: Int) {
        mModel.postSetRepeatMode(mode)
        changeMode(context, mode)
    }

    override fun skipNext() {
        mModel.postSkipNext()
    }

    override fun skipToPosition(id: Long?) {
        mModel.postSkipToPosition(id)
    }

    override fun skipPrevious() {
        mModel.postSkipPrevious()
    }

    private fun refresh(context: Context) {
        if (isScanning) {
            return
        }
        PrintLog.i("开始扫描本地媒体。。。。。。")
        isScanning = true
        mModel.refreshQueue(context, object : ScanMusicFile.AbstractOnScanComplete() {
            override fun onComplete(paths: ArrayList<String>) {
                isScanning = false
                Log.e(TAG, "onComplete: 扫描出来的" + paths.toString())
                mView?.get()?.refreshQueue(true)
                mView?.get()?.hideLoading()
            }
        })
    }

    override fun detachViewAndModel() {
        super.detachViewAndModel()
        mModel.shutdown()
    }

    companion object {
        private const val TAG = "MediaPresenterImpl"
        private const val CACHE_MODE_NAME = "CACHE_MODE"
        private const val CACHE_MODE_KEY = "MODE_KEY"
        private const val CACHE_ROOT_URI = "root_uri"
        private const val CACHE_CHILD_URI = "child_uri"
    }

}
