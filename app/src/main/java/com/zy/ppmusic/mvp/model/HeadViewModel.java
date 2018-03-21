package com.zy.ppmusic.mvp.model;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

/**
 * @author y-slience
 * @date 2018/3/9
 * 如果需要context则继承AndroidViewModel
 */
public class HeadViewModel extends ViewModel{
    private MutableLiveData<Boolean> mPlayStateLiveData = new MutableLiveData<>();

    public void setPlayState(boolean isPlaying){
        mPlayStateLiveData.setValue(isPlaying);
    }

    public MutableLiveData<Boolean> getPlayState(){
        return mPlayStateLiveData;
    }
}
