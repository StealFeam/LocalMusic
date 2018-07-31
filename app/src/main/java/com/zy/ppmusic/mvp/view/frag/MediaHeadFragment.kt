package com.zy.ppmusic.mvp.view.frag

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.zy.ppmusic.App
import com.zy.ppmusic.R
import com.zy.ppmusic.mvp.model.HeadViewModel
import com.zy.ppmusic.utils.PrintLog
import java.lang.ref.WeakReference

/**
 * @author ZhiTouPC
 * @date 2017/12/26
 * 显示媒体专辑图片
 */
class MediaHeadFragment : Fragment() {
    private val mImageLoadOptions: RequestOptions = RequestOptions()
            //不能指定其他缓存
            //glide加载bitmap无法缓存
            .circleCrop()
    private var mObserver: PlayStateObserver? = null
    private var mViewModel: HeadViewModel? = null
    private var mHeadImageView: ImageView? = null
    private val mPauseProgress = 0f

//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//        if (activity != null) {
//            if (mViewModel == null) {
//                val provider = ViewModelProvider(activity!!,
//                        ViewModelProvider.AndroidViewModelFactory(App.getInstance()))
//                mViewModel = provider.get(HeadViewModel::class.java)
//                if (mObserver == null) {
//                    mObserver = PlayStateObserver(this)
//                }
//                mViewModel!!.playState.observe(this, mObserver!!)
//                PrintLog.w("开始监听数据。。。。。")
//            }
//        }
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_media_head, container, false)
        mHeadImageView = rootView.findViewById(R.id.iv_media_head)
        try {
            val arguments = arguments
            var info: MediaMetadataCompat? = null
            if (arguments != null) {
                info = arguments.getParcelable(PARAM)
            }
            if (info != null) {
                val description = info.description
                val iconBitmap = description.iconBitmap
                if (iconBitmap != null) {
                    try {
                        Glide.with(this).load(iconBitmap)
                                .apply(mImageLoadOptions).into(mHeadImageView!!)
                    } catch (e: Exception) {
                        mHeadImageView!!.setImageResource(R.mipmap.ic_music_launcher_round)
                    }

                } else {
                    mHeadImageView!!.setImageResource(R.mipmap.ic_music_launcher_round)
                }
            } else {
                mHeadImageView!!.setImageResource(R.mipmap.ic_music_launcher_round)
            }
        } catch (e: Exception) {
            mHeadImageView!!.setImageResource(R.mipmap.ic_music_launcher_round)
            Log.e(TAG, "onCreateView: exception load .. " + e.message)
        }

        return rootView
    }

    fun changeState(isPlaying: Boolean) {
        PrintLog.w("changeState.........$isPlaying")
        //当前旋转存在一个问题：滑动的时候另一个position的图片也在转动，需要添加滑动时暂停旋转，并且纪录当前的旋转角度
        //        if(isPlaying){
        //            startRotate();
        //        }else{
        //            endRotate();
        //        }
    }

    private fun endRotate() {
        val animation = mHeadImageView!!.animation
        animation?.cancel()
        mHeadImageView!!.clearAnimation()
    }

    private fun startRotate() {
        endRotate()
        val rotateAnimation = RotateAnimation(mPauseProgress, mPauseProgress + 360,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f)
        rotateAnimation.duration = 20000
        rotateAnimation.interpolator = LinearInterpolator()
        rotateAnimation.fillAfter = true
        rotateAnimation.repeatCount = -1
        mHeadImageView!!.startAnimation(rotateAnimation)
    }

//    override fun onDetach() {
//        super.onDetach()
//        if (mViewModel != null) {
//            mViewModel!!.playState.removeObserver(mObserver!!)
//            if (activity != null) {
//                mViewModel!!.playState.removeObservers(activity!!)
//            }
//        }
//    }

    private class PlayStateObserver constructor(fragment: MediaHeadFragment) : Observer<Boolean> {
        private val mFragmentWeak: WeakReference<MediaHeadFragment> = WeakReference(fragment)

        override fun onChanged(aBoolean: Boolean?) {
            aBoolean?.let {
                mFragmentWeak.get()?.changeState(it)
            }
        }
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
