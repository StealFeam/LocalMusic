package com.zy.ppmusic.mvp.view.frag;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.zy.ppmusic.R;

import java.io.ByteArrayOutputStream;

/**
 * @author ZhiTouPC
 * @date 2017/12/26
 */

public class MediaHeadFragment extends Fragment {
    private static final String TAG = "MediaHeadFragment";
    private static final String PARAM = "PARAM";
    private RequestOptions mImageLoadOptions;
    private ByteArrayOutputStream bitmapByteOutStream;


    public MediaHeadFragment() {
        this.mImageLoadOptions = new RequestOptions();
        this.mImageLoadOptions.circleCrop();
        this.mImageLoadOptions.skipMemoryCache(true);
        this.mImageLoadOptions.diskCacheStrategy(DiskCacheStrategy.NONE);
        this.bitmapByteOutStream = new ByteArrayOutputStream();
    }

    public static MediaHeadFragment createInstance(MediaMetadataCompat info) {
        Bundle extra = new Bundle();
        extra.putParcelable(PARAM, info);
        MediaHeadFragment fragment = new MediaHeadFragment();
        fragment.setArguments(extra);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_media_head, container, false);
        AppCompatImageView imageView = rootView.findViewById(R.id.iv_media_head);
        try {
            Bundle arguments = getArguments();
            MediaMetadataCompat info = arguments.getParcelable(PARAM);
            if (info != null) {
                MediaDescriptionCompat description = info.getDescription();
                Bitmap iconBitmap = description.getIconBitmap();
                if (iconBitmap != null) {
                    iconBitmap.compress(Bitmap.CompressFormat.PNG, 50, bitmapByteOutStream);
                    Glide.with(this).load(bitmapByteOutStream.toByteArray()).apply(mImageLoadOptions).into(imageView);
                } else {
                    imageView.setImageResource(R.mipmap.ic_music_launcher_round);
                }
            } else {
                imageView.setImageResource(R.mipmap.ic_music_launcher_round);
            }
        } catch (Exception e) {
            imageView.setImageResource(R.mipmap.ic_music_launcher_round);
            Log.e(TAG, "onCreateView: exception load .. " + e.getMessage());
        }
        return rootView;
    }
}
