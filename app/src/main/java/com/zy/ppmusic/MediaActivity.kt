package com.zy.ppmusic

import android.content.ComponentName
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AppCompatActivity
import com.zy.ppmusic.service.MediaService
import com.zy.ppmusic.utils.ScanMusicFile
import java.util.*

class MediaActivity : AppCompatActivity() {
    private var mMediaBrowser: MediaBrowserCompat? = null
    var mMediaId: String? = null
    var mediaController:MediaControllerCompat?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)
        val serviceComponentName = ComponentName(this, MediaService::class.java)
        mMediaBrowser = MediaBrowserCompat(this, serviceComponentName, mConnectionCallBack, null)
        val btnNext = findViewById(R.id.next)
        btnNext.setOnClickListener({
            mediaController!!.transportControls.skipToNext()
        })

        val btnPlay = findViewById(R.id.play)
        btnPlay.setOnClickListener({
            mediaController!!.transportControls.play()
        })

        val btnPre = findViewById(R.id.previous)
        btnPre.setOnClickListener({
            mediaController!!.transportControls.skipToPrevious()
        })
    }

    override fun onStart() {
        super.onStart()
        mMediaBrowser!!.connect()
    }

    override fun onPause() {
        super.onPause()
        mMediaBrowser!!.disconnect()
    }

    /**
     * 连接状态
     */
    val mConnectionCallBack = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            if (mMediaId == null) {
                mMediaId = mMediaBrowser!!.root
            }
            mMediaBrowser!!.subscribe(mMediaId!!, subscriptionCallBack)
            mediaController = MediaControllerCompat(this@MediaActivity, mMediaBrowser!!.sessionToken)
            MediaControllerCompat.setMediaController(this@MediaActivity, mediaController)

            ScanMusicFile.getInstance().scanMusicFile(this@MediaActivity).setOnScanComplete(object : ScanMusicFile.OnScanComplete() {
                override fun onComplete(paths: ArrayList<String>?) {
                    println("MediaActivity called onScanComplete")
                    val str = paths!![0].hashCode()
                    val extra = Bundle()
                    extra.putStringArrayList("extra", paths)
                    mediaController!!.transportControls.playFromMediaId(str.toString(), extra)
                }
            })
            mediaController!!.registerCallback(mControllerCallBack)
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            val mediaController = MediaControllerCompat.getMediaController(this@MediaActivity)
            if (mediaController != null) {
                mediaController.unregisterCallback(mControllerCallBack)
                MediaControllerCompat.setMediaController(this@MediaActivity, null)
            }
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
        }
    }
    /**
     *
     */
    val subscriptionCallBack = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
        }

        override fun onError(parentId: String) {
            super.onError(parentId)
        }
    }
    /**
     * 媒体控制
     */
    val mControllerCallBack = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
            queue!!.forEachIndexed { _, queueItem ->
                println(queueItem.toString())
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            println("onRepeatModeChanged.....")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            println("onPlaybackStateChanged.....")
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            println("onSessionEvent.....")
        }
    }
}
