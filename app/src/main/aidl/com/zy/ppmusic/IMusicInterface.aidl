package com.zy.ppmusic;
import com.zy.ppmusic.MusicInfoEntity;
import com.zy.ppmusic.IPlayerStateChangeListener;
import com.zy.ppmusic.IOnMusicChangeListener;

interface IMusicInterface {
    void initPlayer(int posi,int seek);
    void playOrPause();
    void next();
    void previous();
    MusicInfoEntity getMusicInfo();
    void registerListener(IPlayerStateChangeListener l);
    void unregisterListener(IPlayerStateChangeListener l);

    void registerMusicChange(IOnMusicChangeListener l);
    void unregisterMusicChange(IOnMusicChangeListener l);
}
