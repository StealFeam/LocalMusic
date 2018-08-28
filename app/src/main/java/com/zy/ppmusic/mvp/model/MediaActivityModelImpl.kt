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
import com.zy.ppmusic.utils.DataTransform
import com.zy.ppmusic.utils.FileUtils
import com.zy.ppmusic.utils.ScanMusicFile
import com.zy.ppmusic.utils.TaskPool
import java.lang.ref.WeakReference

/**
 * @author ZhiTouPC
 */
class MediaActivityModelImpl : IMediaActivityContract.IMediaActivityModel {
    override fun getLocalMode(c: Context,e:(Int) -> Unit){
        TaskPool.execute(Runnable {
            initCachePreference(c)
            val mode = mCachePreference?.getInt(CACHE_MODE_KEY, PlaybackStateCompat.REPEAT_MODE_NONE)?:0
            mMainHandler.post { e.invoke(mode) }
        })
    }

    private var mControllerWeak: WeakReference<MediaControllerCompat>? = null
    private val mMainHandler = Handler(Looper.getMainLooper())

    override fun attachController(controller: MediaControllerCompat?) {
        controller?.apply {
            mControllerWeak = WeakReference(controller)
        }
    }

    override fun refreshQueue(context: Context, l: ScanMusicFile.AbstractOnScanComplete) {
        ScanMusicFile.get().setOnScanComplete(l).scanMusicFile(context)
    }

    override fun loadLocalData(path: String, callback: IMediaActivityContract.IMediaActivityModel.IOnLocalDataLoadFinished) {
        TaskPool.execute(Runnable {
            val data = FileUtils.readObject(path)
            data?.apply {
                DataTransform.get().transFormData(data as ArrayList<MusicInfoEntity>)
            }
            mMainHandler.post {
                callback.callBack(data)
            }
        })
    }

    override fun postSendCommand(method: String, params: Bundle,
                                 resultReceiver: ResultReceiver) {
        TaskPool.execute(Runnable {
            mControllerWeak?.get()?.sendCommand(method, params, resultReceiver)
        })
    }

    override fun postPlayWithId(mediaId: String, extra: Bundle) {
        TaskPool.execute(Runnable {
            mControllerWeak?.get()?.transportControls?.playFromMediaId(mediaId, extra)
        })
    }

    override fun postSendCustomAction(action: String, extra: Bundle) {
        mControllerWeak?.get()?.transportControls?.sendCustomAction(action, extra)
    }

    @Volatile
    private var mCachePreference: SharedPreferences? = null
    private val CACHE_MODE_NAME = "cache_mode"
    private val CACHE_MODE_KEY = "mode_key"

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
        val edit = mCachePreference!!.edit()
        edit.putInt(CACHE_MODE_KEY, mode)
        edit.apply()
    }

    override fun postSetRepeatMode(c: Context, mode: Int) {
        TaskPool.execute(Runnable {
            mControllerWeak?.get()?.transportControls?.setRepeatMode(mode)
            changeLocalMode(c, mode)
        })
    }

    override fun postSkipNext() {
        mControllerWeak?.get()?.transportControls?.let {
            TaskPool.execute(Runnable {
                it.skipToNext()
            })
        }
    }

    override fun postSkipPrevious() {
        TaskPool.execute(Runnable {
            mControllerWeak?.get()?.transportControls?.skipToPrevious()
        })
    }

    override fun postSkipToPosition(id: Long) {
        TaskPool.execute(Runnable {
            mControllerWeak?.get()?.transportControls?.skipToQueueItem(id)
        })
    }

    override fun shutdown() {
        mControllerWeak?.clear()
    }

}
