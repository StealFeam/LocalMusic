package com.zy.ppmusic.mvp.view.frag

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.media.MediaMetadataCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.zy.ppmusic.R

/**
 * @author stealfeam
 * @date 2017/12/26
 * 显示媒体专辑图片
 */
class MediaHeadFragment : Fragment() {
    private val mImageLoadOptions: RequestOptions = RequestOptions()
            //不能指定其他缓存
            //glide加载bitmap无法缓存
            .circleCrop()
    private var mHeadImageView: ImageView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_media_head, container, false)
        mHeadImageView = rootView.findViewById(R.id.iv_media_head)
        val info: MediaMetadataCompat? = arguments?.getParcelable(PARAM)
        info?.description?.iconBitmap?.apply {
            Glide.with(rootView).load(this)
                    .apply(mImageLoadOptions).into(mHeadImageView!!)
        }?:apply {
            mHeadImageView?.setImageResource(R.mipmap.ic_music_launcher_round)
        }
        return rootView
    }

    companion object {
        private const val TAG = "MediaHeadFragment"
        private const val PARAM = "PARAM"

        fun createInstance(info: MediaMetadataCompat?): MediaHeadFragment {
            val extra = Bundle()
            extra.putParcelable(PARAM, info)
            val fragment = MediaHeadFragment()
            fragment.arguments = extra
            return fragment
        }
    }
}
