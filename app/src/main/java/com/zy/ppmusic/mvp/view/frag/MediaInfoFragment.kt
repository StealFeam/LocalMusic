package com.zy.ppmusic.mvp.view.frag

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.media.MediaMetadataCompat
import android.support.v7.widget.AppCompatImageView
import android.support.v7.widget.AppCompatTextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.zy.ppmusic.R

/**
 * @author ZhiTouPC
 * @date 2017/11/28
 */
class MediaInfoFragment : Fragment() {

    companion object {
        public val EXTRA = "EXTRA"

        fun createInstance(info: MediaMetadataCompat?): MediaInfoFragment {
            val extra = Bundle()
            extra.putParcelable(EXTRA, info)
            val frag = MediaInfoFragment()
            frag.arguments = extra
            return frag
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater?.inflate(R.layout.frag_media_info, container, false)
        val ivInfo = rootView?.findViewById<AppCompatImageView>(R.id.iv_media_info)
        val tvNameInfo = rootView?.findViewById<AppCompatTextView>(R.id.tv_media_info_name)
        val tvAuthorInfo = rootView?.findViewById<AppCompatTextView>(R.id.tv_media_info_author)
        val extra = arguments.getParcelable<MediaMetadataCompat>(EXTRA)
        val requestOptions = RequestOptions()
        requestOptions.fitCenter()
        if (extra != null) {
            val displayTitle = extra.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)
            val subTitle = extra.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE)
            tvNameInfo?.text = displayTitle
            tvAuthorInfo?.text = subTitle
            if (extra.description.iconBitmap == null) {
                Glide.with(this).load(R.mipmap.ic_music_launcher_round).apply(requestOptions).into(ivInfo)
            } else {
                ivInfo?.setImageBitmap(extra.description.iconBitmap)
            }
        } else {
            Glide.with(this).load(R.mipmap.ic_music_launcher_round).apply(requestOptions).into(ivInfo)
            tvNameInfo?.text = getString(R.string.app_name)
            tvAuthorInfo?.text = getString(R.string.app_name)
        }
        return rootView
    }
}