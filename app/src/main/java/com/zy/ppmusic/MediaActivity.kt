package com.zy.ppmusic

import android.content.ComponentName
import android.media.browse.MediaBrowser
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.zy.ppmusic.adapter.PlayQueueAdapter
import com.zy.ppmusic.service.MediaService

class MediaActivity : AppCompatActivity() {
    private var mMediaBrowser: MediaBrowserCompat? = null
    var mMediaId: String? = null
    var mediaController: MediaControllerCompat? = null
    var playQueue: MutableList<MediaBrowserCompat.MediaItem>? = null
    var ivNextAction: ImageView? = null
    var ivPlayAction: ImageView? = null
    var ivMenuAction: ImageView? = null
    var tvDisPlayName: TextView? = null
    var tvSubName: TextView? = null
    var queueRecycle:RecyclerView?=null
    var queueDialog:BottomSheetDialog?=null
    var queueAdapter:PlayQueueAdapter?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)
        val serviceComponentName = ComponentName(this, MediaService::class.java)
        mMediaBrowser = MediaBrowserCompat(this, serviceComponentName, mConnectionCallBack, null)

        ivNextAction = findViewById(R.id.control_action_next) as ImageView
        ivPlayAction = findViewById(R.id.control_action_play_pause) as ImageView
        ivMenuAction = findViewById(R.id.control_action_show_queue) as ImageView

        tvDisPlayName = findViewById(R.id.control_display_title) as TextView

        tvSubName = findViewById(R.id.control_display_sub_title) as TextView

        ivNextAction!!.setOnClickListener({
            mediaController!!.transportControls.skipToNext()
        })

        ivPlayAction!!.setOnClickListener({
            mediaController!!.transportControls.play()
        })

        ivMenuAction!!.setOnClickListener({
            queueDialog = BottomSheetDialog(this@MediaActivity)
            val contentView = LayoutInflater.from(this@MediaActivity).inflate(R.layout.play_queue_layout,null)
            queueDialog!!.setContentView(contentView)
            queueRecycle = contentView.findViewById(R.id.control_queue_recycle) as RecyclerView
            queueAdapter = PlayQueueAdapter(playQueue)
            queueAdapter!!.setOnQueueItemClickListener { obj, position ->
                if(obj is MediaBrowserCompat.MediaItem){
                    mediaController!!.transportControls.playFromMediaId(obj.mediaId,null)
                }
            }
            queueRecycle!!.adapter = queueAdapter
            queueRecycle!!.layoutManager = LinearLayoutManager(this)
            queueDialog!!.show()
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
        //service加载完成列表回调
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
            if(queueAdapter != null){
                queueAdapter!!.setData(children)
            }
            playQueue = children
            val extra = Bundle()
            extra.putInt("position", 0)
            mediaController!!.transportControls.playFromMediaId(playQueue!![0].mediaId, extra)
        }

        //播放列表加载失败
        override fun onError(parentId: String) {
            super.onError(parentId)
        }
    }
    /**
     * 媒体控制
     */
    val mControllerCallBack = object : MediaControllerCompat.Callback() {
        //当前歌曲信息变化回调
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            println("onMetadataChanged called")
            if(metadata != null){
                val displayTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
                val subTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE)
                tvDisPlayName!!.text = displayTitle
                tvSubName!!.text = subTitle
            }
        }

        //播放列表变化回调
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

        //播放器状态改变回调
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            println("onPlaybackStateChanged.....")
            when(state!!.state){
                PlaybackStateCompat.STATE_PLAYING->{
                    ivPlayAction!!.setImageResource(R.drawable.ic_black_pause)
                }
                PlaybackStateCompat.STATE_PAUSED->{
                    ivPlayAction!!.setImageResource(R.drawable.ic_black_play)
                }
                PlaybackStateCompat.STATE_ERROR->{
                    ivPlayAction!!.setImageResource(R.drawable.ic_black_play)
                }
            }
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            println("onSessionEvent.....")
        }

        override fun onExtrasChanged(extras: Bundle?) {
            super.onExtrasChanged(extras)
            println("onExtrasChanged.....")
        }
    }
}
