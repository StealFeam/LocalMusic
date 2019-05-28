package com.zy.ppmusic.widget;

import androidx.annotation.NonNull;

/**
 * @author stealfeam
 * @since 2018/10/24
 */
public class ProgressType implements ITouchShowType {

    @Override
    @NonNull
    public String getDrawContent(float progress) {
        return String.valueOf((int)(progress * 120));
    }
}
