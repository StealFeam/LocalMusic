package com.zy.ppmusic.utils;

import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;

/**
 * @author ZhiTouPC
 * @date 2017/12/25
 */

public class VisualizerUtils {
    private static VisualizerUtils instance = new VisualizerUtils();
    private MediaPlayer mediaPlayer;
    private Visualizer visualizer;

    public VisualizerUtils() {
    }

    private static VisualizerUtils get() {
        return instance;
    }

    private void attachPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
        try {
            visualizer = new Visualizer(mediaPlayer.getAudioSessionId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setListener(Visualizer.OnDataCaptureListener l,int rate,boolean b1,boolean b2){
        if (visualizer != null) {
            visualizer.setDataCaptureListener(l,rate,b1,b2);
        }
    }


}
