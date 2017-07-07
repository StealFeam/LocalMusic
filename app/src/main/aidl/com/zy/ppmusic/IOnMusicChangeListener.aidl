// IOnMusicChangeListener.aidl
package com.zy.ppmusic;

// Declare any non-default types here with import statements
import com.zy.ppmusic.MusicInfoEntity;

interface IOnMusicChangeListener {
    void onMusicChange(in MusicInfoEntity entity);
}
