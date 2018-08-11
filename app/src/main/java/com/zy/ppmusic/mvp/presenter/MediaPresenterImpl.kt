package com.zy.ppmusic.mvp.presenter

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.*
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.zy.ppmusic.entity.MusicInfoEntity
import com.zy.ppmusic.mvp.contract.IMediaActivityContract
import com.zy.ppmusic.mvp.model.MediaActivityModelImpl
import com.zy.ppmusic.utils.DataTransform
import com.zy.ppmusic.utils.FileUtils
import com.zy.ppmusic.utils.PrintLog
import com.zy.ppmusic.utils.ScanMusicFile
import java.lang.ref.WeakReference

/**
 * @author ZhiTouPC
 */
class MediaPresenterImpl(view: IMediaActivityContract.IMediaActivityView) :
        IMediaActivityContract.AbstractMediaActivityPresenter(view) {
    private var mCachePreference: SharedPreferences? = null
    private var isScanning = false
    private val mMainHandler = Handler(Looper.getMainLooper())

    private val finishedListener = object : OnTaskFinishedListener {
        override fun onRefreshQueue(paths: ArrayList<String>?) {
            mView.get()?.refreshQueue(paths, true)
            mView.get()?.hideLoading()
        }

        override fun loadFinished() {
            mView.get()?.loadFinished()
            mView.get()?.hideLoading()
        }
    }
    override fun getChildrenUri(): String {
        return mCachePreference?.getString(CACHE_CHILD_URI, "") ?: ""
    }

    override fun getGrantedRootUri(): String {
        return mCachePreference?.getString(CACHE_ROOT_URI, "") ?: ""
    }

    override fun setGrantedRootUri(uri: String,child:String) {
        mCachePreference?.edit()?.putString(CACHE_ROOT_URI, uri)?.putString(CACHE_CHILD_URI,child)?.apply()
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
            refresh(context, true)
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
                                data?.let {
                                    PrintLog.i("读取到本地缓存数据")
                                    val builder = TransFormTask.Builder()
                                    builder.listener = finishedListener
                                    builder.isRefresh = false
                                    builder.musicInfoEntities = data as ArrayList<MusicInfoEntity>
                                    builder.context = context
                                    builder.executeTask()
                                } ?: let {
                                    //回到主线程
                                    mMainHandler.post {
                                        PrintLog.i("未读取到本地数据")
                                        refresh(context, false)
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
        if(mCachePreference!!.getInt(CACHE_MODE_KEY, PlaybackStateCompat.REPEAT_MODE_NONE) == mode){
            return
        }
        val edit = mCachePreference!!.edit()
        edit.putInt(CACHE_MODE_KEY, mode)
        edit.apply()
    }

    override fun getLocalMode(c: Context): Int {
        initCachePreference(c)
        return mCachePreference?.getInt(CACHE_MODE_KEY, PlaybackStateCompat.REPEAT_MODE_NONE)?:PlaybackStateCompat.REPEAT_MODE_NONE
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

    private fun refresh(context: Context, isRefresh: Boolean) {
        if (isScanning) {
            return
        }
        PrintLog.i("开始扫描本地媒体。。。。。。")
        isScanning = true
        mModel.refreshQueue(context, object : ScanMusicFile.AbstractOnScanComplete() {
            override fun onComplete(paths: ArrayList<String>) {
                isScanning = false
                Log.e(TAG, "onComplete: 扫描出来的" + paths.toString())
                val builder = TransFormTask.Builder()
                builder.context = context
                builder.isRefresh = isRefresh
                builder.mPaths = paths
                builder.listener = finishedListener
                builder.executeTask()
            }
        })
    }

    private interface OnTaskFinishedListener {

        /**
         * 完成时回调
         *
         * @param paths 路径集合
         */
        fun onRefreshQueue(paths: ArrayList<String>?)

        /**
         * 加载完成
         */
        fun loadFinished()
    }

    /**
     * 负责将本地数据或者扫描到的数据数据转换
     */
    private class TransFormTask private constructor(builder: Builder) :
            AsyncTask<String?, Void, ArrayList<String>?>() {
        private val mContextWeak: WeakReference<Context>
        private val mPaths: ArrayList<String>?
        private val entities: ArrayList<MusicInfoEntity>?
        private val isRefresh: Boolean
        private val listener: OnTaskFinishedListener?

        init {
            this.isRefresh = builder.isRefresh
            this.listener = builder.listener
            this.mContextWeak = WeakReference<Context>(builder.context)
            this.mPaths = builder.mPaths
            this.entities = builder.musicInfoEntities
        }

        override fun doInBackground(vararg paths: String?): ArrayList<String>? {
            if (mPaths == null) {
                entities?.let {
                    DataTransform.get().transFormData(it)
                }
            } else {
                mContextWeak.get()?.let {
                    DataTransform.get().transFormData(it, mPaths)
                }
            }
            return mPaths
        }

        override fun onPostExecute(strings: ArrayList<String>?) {
            super.onPostExecute(strings)
            if (listener != null) {
                if (isRefresh) {
                    listener.onRefreshQueue(strings)
                } else {
                    listener.loadFinished()
                }
            }
        }

        class Builder {
            var context: Context? = null
            var mPaths: ArrayList<String>? = null
            var isRefresh: Boolean = false
            var musicInfoEntities: ArrayList<MusicInfoEntity>? = null
            var listener: OnTaskFinishedListener? = null

            fun executeTask() {
                val task = TransFormTask(this)
                task.execute()
            }
        }
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
