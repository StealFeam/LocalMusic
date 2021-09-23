package com.zy.ppmusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.zy.ppmusic.R
import com.zy.ppmusic.utils.DataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.ArrayList

/**
 * @author stealfeam
 * @date 2017/12/26
 */
class MediaAlbumAdapter : RecyclerView.Adapter<MediaAlbumAdapter.MediaHolder>() {
    private val loadOptionsForImage: RequestOptions = RequestOptions()
            .circleCrop()
    private val pathList: ArrayList<String> by lazy { ArrayList() }

    fun fillDataToAdapter(context: CoroutineScope, newPathList: List<String>?) {
        if (newPathList == null) {
            return
        }
        context.launch(Dispatchers.Main) {
            val diffResult: DiffUtil.DiffResult = withContext(Dispatchers.IO) {
                getDiffResult(newPathList)
            }
            diffResult.dispatchUpdatesTo(this@MediaAlbumAdapter)
            pathList.clear()
            pathList.addAll(newPathList)
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
        val mediaId = DataProvider.get().mediaIdList[position]
        val metadataCompat = DataProvider.get().getMetadataItem(mediaId)
        metadataCompat?.description?.iconBitmap?.apply {
            Glide.with(holder.albumImageView).load(this)
                    .apply(loadOptionsForImage).into(holder.albumImageView)
        } ?: apply {
            holder.albumImageView.setImageResource(R.mipmap.ic_music_launcher_round)
        }
    }
}
