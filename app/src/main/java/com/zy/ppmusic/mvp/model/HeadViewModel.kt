package com.zy.ppmusic.mvp.model

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

/**
 * @author y-slience
 * @date 2018/3/9
 * 如果需要context则继承AndroidViewModel
 */
class HeadViewModel : ViewModel() {
    private val playState = MutableLiveData<Boolean>()

    fun setPlayState(isPlaying: Boolean) {
        playState.value = isPlaying
    }
}
