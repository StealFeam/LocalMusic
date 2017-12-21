package com.zy.ppmusic.adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import com.zy.ppmusic.mvp.view.frag.MediaInfoFragment
import com.zy.ppmusic.utils.DataTransform

/**
 * @author ZhiTouPC
 * @date 2017/11/28
 */
class MediaInfoAdapter(manager: FragmentManager, pathList: List<String>) : FragmentStatePagerAdapter(manager) {
    private var mPathList: List<String>? = null
    private var mIsNotifyChanged: Boolean? = null

    init {
        this.mPathList = pathList
    }

    fun setPathList(pathList: List<String>) {
        this.mPathList = pathList
        println("notify changed count=" + pathList.size)
        notifyDataSetChanged()
    }

    override fun notifyDataSetChanged() {
        //查看ViewPager中1030行代码得知只有getItemPosition()返回了POSITION_NONE，才会删除所有的item
        mIsNotifyChanged = true
        super.notifyDataSetChanged()
        mIsNotifyChanged = false
    }

    override fun getItemPosition(`object`: Any?): Int {
        return if (mIsNotifyChanged!!.not()) {
            super.getItemPosition(`object`)
        } else {
            PagerAdapter.POSITION_NONE
        }
    }


    override fun getItem(position: Int): Fragment = createFragmentByInfo(position)

    private fun createFragmentByInfo(position: Int): Fragment {
        return if (mPathList == null || mPathList?.size == 0) {
            MediaInfoFragment.createInstance(null)
        } else {
            val mediaId = DataTransform.getInstance().mediaIdList[position]
            MediaInfoFragment.createInstance(DataTransform.getInstance().getMetadataItem(mediaId))
        }
    }

    override fun getCount(): Int {
        return if (mPathList == null || mPathList?.size == 0) {
            1
        } else {
            mPathList!!.size
        }
    }

}