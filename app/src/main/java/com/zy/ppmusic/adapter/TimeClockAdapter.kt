package com.zy.ppmusic.adapter

import android.view.Gravity
import android.widget.TextView
import com.zy.ppmusic.R
import com.zy.ppmusic.adapter.base.AbstractSingleTypeAdapter
import com.zy.ppmusic.adapter.base.ExpandableViewHolder
import com.zy.ppmusic.adapter.base.OnItemViewClickListener
import java.util.*

/**
 * @author ZY
 */
class TimeClockAdapter : AbstractSingleTypeAdapter() {
    private val array: MutableList<Int>?

    var isTick = false
        private set

    private var mClickListener: OnItemViewClickListener? = null

    init {
        this.array = ArrayList()
        val mCount = 5
        for (i in 0 until mCount) {
            this.array.add((i + 1) * 15)
        }
        this.array.add(120)
    }

    /**
     * 是否正在倒计时
     */
    fun setTicking(flag: Boolean) {
        if (isTick == flag) {
            return
        }
        isTick = flag
        notifyDataSetChanged()
    }

    fun getItem(position: Int): Int {
        return array!![position]
    }

    fun setOnItemClickListener(l: OnItemViewClickListener) {
        this.mClickListener = l
    }

    override fun bindHolderData(holder: ExpandableViewHolder, viewType: Int) {
        super.bindHolderData(holder, viewType)
        holder.attachOnClickListener(mClickListener, holder.itemView)
    }

    override fun setupItemData(holder: ExpandableViewHolder, position: Int) {
        val tvTime = holder.getView<TextView>(R.id.item_normal_text)
        tvTime!!.gravity = Gravity.CENTER
        if (isTick) {
            if (position == 0) {
                tvTime.text = "关闭定时"
            } else {
                tvTime.text = String.format(Locale.CHINA, "%d分钟", array!![position - 1])
            }
        } else {
            tvTime.text = String.format(Locale.CHINA, "%d分钟", array!![position])
        }
    }

    override fun itemCount(): Int {
        if (array == null) {
            return 0
        }
        return if (isTick) array.size + 1 else array.size
    }

    override fun getItemLayoutId(): Int {
        return R.layout.item_list_normal
    }

}
