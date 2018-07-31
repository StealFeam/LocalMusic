package com.zy.ppmusic.callback

/**
 * @author ZhiTouPC
 * @date 2018/1/19
 */

interface TimeTikCallBack {
    /**
     * 更新时间
     * @param mis 剩余时间（毫秒）
     */
    fun onTik(mis: Long)
}
