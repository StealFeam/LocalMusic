package com.zy.ppmusic.adapter

import com.zy.ppmusic.mvp.view.frag.MediaHeadFragment
import com.zy.ppmusic.utils.DataProvider
import java.util.ArrayList

/**
 * @author stealfeam
 * @date 2017/12/26
 */
class MediaHeadAdapter(fm: androidx.fragment.app.FragmentManager, pathList: List<String>) : androidx.fragment.app.FragmentStatePagerAdapter(fm) {
    private val mPathList: MutableList<String>
    private var isInit = true

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
        isInit = false
        notifyDataSetChanged()
    }

    override fun getItemPosition(`object`: Any): Int {
        if(!isInit){
            return POSITION_NONE
        }
        return super.getItemPosition(`object`)
    }

    override fun getItem(position: Int): androidx.fragment.app.Fragment {
        if (this.mPathList.isEmpty()) {
            return MediaHeadFragment.createInstance(null)
        }
        val mediaId = DataProvider.get().mediaIdList[position]
        val metadataCompat = DataProvider.get().getMetadataItem(mediaId)
        return MediaHeadFragment.createInstance(metadataCompat)
    }


    override fun getCount(): Int {
        return this.mPathList.size
    }
}
