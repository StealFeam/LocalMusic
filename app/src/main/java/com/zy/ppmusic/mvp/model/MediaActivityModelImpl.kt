package com.zy.ppmusic.mvp.model

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.zy.ppmusic.entity.MusicInfoEntity
import com.zy.ppmusic.mvp.contract.IMediaActivityContract
import com.zy.ppmusic.utils.*
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/**
 * @author stealfeam
 */
class MediaActivityModelImpl : IMediaActivityContract.IMediaActivityModel {
    private val CACHE_MODE_NAME = "cache_mode"
    private val CACHE_MODE_KEY = "mode_key"
    private val CACHE_ROOT_URI = "root_uri"
    private val CACHE_CHILD_URI = "child_uri"

    private val scope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO + Job()) }

    @Volatile
    private var mCachePreference: SharedPreferences? = null
    private var mControllerWeak: WeakReference<MediaControllerCompat>? = null

    private fun initCachePreference(context: Context) {
        if (mCachePreference == null) {
            mCachePreference = context.getSharedPreferences(CACHE_MODE_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun changeLocalMode(c: Context, mode: Int) {
        initCachePreference(c)
        if (mCachePreference!!.getInt(CACHE_MODE_KEY, PlaybackStateCompat.REPEAT_MODE_NONE) == mode) {
            return
        }
        mCachePreference?.edit()?.putInt(CACHE_MODE_KEY, mode)?.apply()
    }

    override fun getGrantedRootUri(): String {
        return mCachePreference?.getString(CACHE_ROOT_URI, "") ?: ""
    }

    override fun getGrantedChildrenUri(): String {
        return mCachePreference?.getString(CACHE_CHILD_URI, "") ?: ""
    }

    override fun saveGrantedUri(root: String, child: String) {
        mCachePreference?.edit()?.apply {
            putString(CACHE_ROOT_URI, root)
            putString(CACHE_CHILD_URI, child)
        }?.apply()
    }

    override fun getLocalMode(c: Context, e: (Int) -> Unit) {
        //创建协程环境
        //CoroutineScope注释代码
        scope.launch(Dispatchers.Main) {
            val job = async(Dispatchers.IO){
                initCachePreference(c)
                return@async mCachePreference?.getInt(CACHE_MODE_KEY, PlaybackStateCompat.REPEAT_MODE_NONE) ?: 0
            }
            val mode = job.await()
            e.invoke(mode)
        }

    }

    override fun removeQueueItem(position: Int) {
        scope.launch(Dispatchers.IO) {
            mControllerWeak?.get()?.removeQueueItem(DataProvider.get().queueItemList.get()[position].description)
        }
    }

    override fun attachController(controller: MediaControllerCompat?) {
        controller?.apply {
            mControllerWeak = WeakReference(controller)
        }
    }

    override fun postSendCommand(method: String, params: Bundle,
                                 resultReceiver: ResultReceiver) {
        scope.launch(Dispatchers.IO) {
            mControllerWeak?.get()?.sendCommand(method, params, resultReceiver)
        }
    }

    override fun postPlayWithId(mediaId: String, extra: Bundle) {
        scope.launch(Dispatchers.IO) {
            mControllerWeak?.get()?.transportControls?.playFromMediaId(mediaId, extra)
        }
    }

    override fun postSendCustomAction(action: String, extra: Bundle) {
        mControllerWeak?.get()?.transportControls?.sendCustomAction(action, extra)
    }


    override fun postSetRepeatMode(c: Context, mode: Int) {
        scope.launch(Dispatchers.IO) {
            mControllerWeak?.get()?.transportControls?.setRepeatMode(mode)
            changeLocalMode(c, mode)
        }
    }

    override fun postSkipNext() {
        mControllerWeak?.get()?.transportControls?.let {
            scope.launch(Dispatchers.IO) {
                it.skipToNext()
            }
        }
    }

    override fun postSkipPrevious() {
        scope.launch(Dispatchers.IO) {
            mControllerWeak?.get()?.transportControls?.skipToPrevious()
        }
    }

    override fun postSkipToPosition(id: Long) {
        scope.launch(Dispatchers.IO) {
            PrintLog.e("Model准备传输-----")
            mControllerWeak?.get()?.transportControls?.skipToQueueItem(id)
        }
    }

    override fun shutdown() {
        scope.cancel()
        mControllerWeak?.clear()
    }

}
