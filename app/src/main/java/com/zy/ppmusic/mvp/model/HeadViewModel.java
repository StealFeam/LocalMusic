package com.zy.ppmusic.mvp.model;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

/**
 * @author y-slience
 * @date 2018/3/9
 */
public class HeadViewModel extends AndroidViewModel{
    private MutableLiveData<Boolean> mPlayStateLiveData = new MutableLiveData<>();

    public HeadViewModel(@NonNull Application application) {
        super(application);
    }

    public void setPlayState(boolean isPlaying){
        mPlayStateLiveData.setValue(isPlaying);
    }

    public MutableLiveData<Boolean> getPlayState(){
        return mPlayStateLiveData;
    }
}
