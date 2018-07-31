package com.zy.ppmusic.adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.media.MediaMetadataCompat
import android.view.ViewGroup

import com.zy.ppmusic.mvp.view.frag.MediaHeadFragment
import com.zy.ppmusic.utils.DataTransform

import java.util.ArrayList

/**
 * @author ZhiTouPC
 * @date 2017/12/26
 */

class MediaHeadAdapter(fm: FragmentManager, pathList: List<String>) : FragmentStatePagerAdapter(fm) {
    private val mPathList: MutableList<String>

    init {
        this.mPathList = ArrayList()
        setPathList(pathList)
    }

    fun setPathList(pathList: List<String>?) {
        if (pathList == null) {
            return
        }
        if (pathList.size == this.mPathList.size) {
            return
        }
        this.mPathList.clear()
        this.mPathList.addAll(pathList)
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): Fragment {
        val zero = 0
        if (this.mPathList.size == zero) {
            return MediaHeadFragment.createInstance(null)
        }
        val mediaId = DataTransform.get().mediaIdList[position]
        val metadataCompat = DataTransform.get().getMetadataItem(mediaId)
        return MediaHeadFragment.createInstance(metadataCompat)
    }

    override fun getCount(): Int {
        return this.mPathList.size
    }
}
