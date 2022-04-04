package com.zy.ppmusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zy.ppmusic.R
import com.zy.ppmusic.utils.DataProvider
import com.zy.ppmusic.utils.lazy2
import kotlinx.coroutines.*
import kotlin.collections.ArrayList

/**
 * @author stealfeam
 * @date 2017/12/26
 */
class MediaAlbumAdapter : RecyclerView.Adapter<MediaAlbumAdapter.MediaHolder>() {
    private val pathList: ArrayList<String> by lazy2 { ArrayList() }
    private var lastFillJob: Job? = null

    fun fillDataToAdapter(context: CoroutineScope, newPathList: List<String>?) {
        if (newPathList == null) {
            return
        }
        lastFillJob?.cancel()
        lastFillJob = context.launch(Dispatchers.Main) {
            val diffResult: DiffUtil.DiffResult = withContext(Dispatchers.IO) {
                getDiffResult(newPathList)
            }
            pathList.clear()
            pathList.addAll(newPathList)
            diffResult.dispatchUpdatesTo(this@MediaAlbumAdapter)
        }
    }

    private class MediaDiffCallback(private val oldPathList: List<String>, private val newPathList: List<String>?) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldPathList.size
        }

        override fun getNewListSize(): Int {
            return newPathList?.size ?: 0
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldPathList[oldItemPosition] === newPathList?.getOrNull(newItemPosition)
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldPathList[oldItemPosition] == newPathList?.getOrNull(newItemPosition)
        }
    }

    private fun getDiffResult(newPathList: List<String>?): DiffUtil.DiffResult {
        return DiffUtil.calculateDiff(MediaDiffCallback(pathList, newPathList))
    }

    override fun getItemCount(): Int {
        return pathList.size
    }

    class MediaHolder(mediaItemView: View) : RecyclerView.ViewHolder(mediaItemView) {
        val albumImageView: ImageView = mediaItemView.findViewById(R.id.iv_media_head)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaHolder {
        return MediaHolder(LayoutInflater.from(parent.context).inflate(R.layout.fragment_media_head, parent, false))
    }

    override fun onBindViewHolder(holder: MediaHolder, position: Int) {
        val mediaId = DataProvider.get().getMediaIdList()[position]
        val metadataCompat = DataProvider.get().getMetadataItem(mediaId)
        metadataCompat?.description?.iconBitmap?.apply {
            Glide.with(holder.albumImageView).load(this).into(holder.albumImageView)
        } ?: apply {
            holder.albumImageView.setImageResource(R.mipmap.ic_music_launcher_round)
        }
    }
}
