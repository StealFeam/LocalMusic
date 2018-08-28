package com.zy.ppmusic.mvp.presenter

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import com.zy.ppmusic.mvp.contract.IMediaActivityContract
import com.zy.ppmusic.mvp.model.MediaActivityModelImpl
import com.zy.ppmusic.utils.*

/**
 * @author ZhiTouPC
 */
class MediaPresenterImpl(view: IMediaActivityContract.IMediaActivityView) :
        IMediaActivityContract.AbstractMediaActivityPresenter(view) {
    private var mCachePreference: SharedPreferences? = null
    private var isScanning = false

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
                mModel.loadLocalData(Constant.CACHE_FILE_PATH,
                        object : IMediaActivityContract.IMediaActivityModel.IOnLocalDataLoadFinished {
                            override fun callBack(data: Any?) {
                                data?.apply {
                                    PrintLog.i("读取到本地缓存数据")
                                    mView?.get()?.loadFinished()
                                    mView?.get()?.hideLoading()
                                } ?: let {
                                    PrintLog.i("未读取到本地数据")
                                    refresh(context)
                                }
                            }
                        })
            }
        }
    }


    override fun getLocalMode(c: Context) {
        mModel.getLocalMode(c, e = {
            mView?.get()?.setRepeatMode(it)
        })
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
        mModel.postSetRepeatMode(context,mode)
    }

    override fun skipNext() {
        mModel.postSkipNext()
    }

    override fun skipToPosition(id: Long) {
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

        private const val CACHE_ROOT_URI = "root_uri"
        private const val CACHE_CHILD_URI = "child_uri"
    }

}
