package com.zy.ppmusic.mvp.model

import android.content.Context
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.MediaControllerCompat

import com.zy.ppmusic.mvp.contract.IMediaActivityContract
import com.zy.ppmusic.utils.FileUtils
import com.zy.ppmusic.utils.ScanMusicFile
import com.zy.ppmusic.utils.TaskPool

import java.lang.ref.WeakReference

/**
 * @author ZhiTouPC
 */
class MediaActivityModelImpl : IMediaActivityContract.IMediaActivityModel {
    private var mControllerWeak: WeakReference<MediaControllerCompat>? = null

    override fun attachController(controller: MediaControllerCompat?) {
        controller?.let {
            mControllerWeak = WeakReference(controller)
        }
    }

    override fun refreshQueue(context: Context, l: ScanMusicFile.AbstractOnScanComplete) {
        ScanMusicFile.get().setOnScanComplete(l).scanMusicFile(context)
    }

    override fun loadLocalData(path: String, callback: IMediaActivityContract.IMediaActivityModel.IOnLocalDataLoadFinished) {
        TaskPool.execute(Runnable {
            val localData = FileUtils.readObject(path)
            callback.callBack(localData)
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

    override fun postSetRepeatMode(mode: Int) {
        TaskPool.execute(Runnable {
            mControllerWeak?.get()?.transportControls?.setRepeatMode(mode)
        })
    }

    override fun postSkipNext() {
        TaskPool.execute(Runnable {
            mControllerWeak?.get()?.transportControls?.skipToNext()
        })
    }

    override fun postSkipPrevious() {
        TaskPool.execute(Runnable {
            mControllerWeak?.get()?.transportControls?.skipToPrevious()
        })
    }

    override fun postSkipToPosition(id: Long?) {
        TaskPool.execute(Runnable {
            mControllerWeak?.get()?.transportControls?.skipToQueueItem(id!!)
        })
    }

    override fun shutdown() {
        mControllerWeak?.clear()
    }

    companion object {
        private val TAG = "MediaMediaActivityModel"
    }

}
