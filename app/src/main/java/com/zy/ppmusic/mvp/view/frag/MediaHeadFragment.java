package com.zy.ppmusic.mvp.view.frag;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.zy.ppmusic.App;
import com.zy.ppmusic.R;
import com.zy.ppmusic.mvp.model.HeadViewModel;
import com.zy.ppmusic.utils.PrintLog;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

/**
 * @author ZhiTouPC
 * @date 2017/12/26
 * 显示媒体专辑图片
 */
public class MediaHeadFragment extends Fragment {
    private static final String TAG = "MediaHeadFragment";
    private static final String PARAM = "PARAM";
    private final RequestOptions mImageLoadOptions;
    private PlayStateObserver mObserver;
    private HeadViewModel mViewModel;
    private ImageView mHeadImageView;
    private float mPauseProgress = 0f;

    public MediaHeadFragment() {
        this.mImageLoadOptions = new RequestOptions()
                //不能指定其他缓存
                //glide加载bitmap无法缓存
                .circleCrop();
    }

    public static MediaHeadFragment createInstance(MediaMetadataCompat info) {
        Bundle extra = new Bundle();
        extra.putParcelable(PARAM, info);
        MediaHeadFragment fragment = new MediaHeadFragment();
        fragment.setArguments(extra);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            if (mViewModel == null) {
                ViewModelProvider provider = new ViewModelProvider(getActivity(),
                        new ViewModelProvider.AndroidViewModelFactory(App.getInstance()));
                mViewModel = provider.get(HeadViewModel.class);
                if (mObserver == null) {
                    mObserver = new PlayStateObserver(this);
                }
                mViewModel.getPlayState().observe(this,mObserver);
                PrintLog.w("开始监听数据。。。。。");
            }
        }
    }



    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_media_head, container, false);
        mHeadImageView = rootView.findViewById(R.id.iv_media_head);
        try {
            Bundle arguments = getArguments();
            MediaMetadataCompat info = null;
            if (arguments != null) {
                info = arguments.getParcelable(PARAM);
            }
            if (info != null) {
                MediaDescriptionCompat description = info.getDescription();
                Bitmap iconBitmap = description.getIconBitmap();
                if (iconBitmap != null) {
                    try {
                        Glide.with(this).load(iconBitmap)
                                .apply(mImageLoadOptions).into(mHeadImageView);
                    }catch (Exception e){
                        mHeadImageView.setImageResource(R.mipmap.ic_music_launcher_round);
                    }
                } else {
                    mHeadImageView.setImageResource(R.mipmap.ic_music_launcher_round);
                }
            } else {
                mHeadImageView.setImageResource(R.mipmap.ic_music_launcher_round);
            }
        } catch (Exception e) {
            mHeadImageView.setImageResource(R.mipmap.ic_music_launcher_round);
            Log.e(TAG, "onCreateView: exception load .. " + e.getMessage());
        }
        return rootView;
    }

    public void changeState(boolean isPlaying){
        PrintLog.w("changeState........."+isPlaying);
        //当前旋转存在一个问题：滑动的时候另一个position的图片也在转动，需要添加滑动时暂停旋转，并且纪录当前的旋转角度
//        if(isPlaying){
//            startRotate();
//        }else{
//            endRotate();
//        }
    }

    private void endRotate() {
        Animation animation = mHeadImageView.getAnimation();
        if (animation != null) {
            animation.cancel();
        }
        mHeadImageView.clearAnimation();
    }

    private void startRotate() {
        endRotate();
        RotateAnimation rotateAnimation = new RotateAnimation(mPauseProgress,mPauseProgress + 360,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,RotateAnimation.RELATIVE_TO_SELF,0.5f);
        rotateAnimation.setDuration(20000);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setFillAfter(true);
        rotateAnimation.setRepeatCount(-1);
        mHeadImageView.startAnimation(rotateAnimation);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mViewModel != null) {
            mViewModel.getPlayState().removeObserver(mObserver);
            if (getActivity() != null) {
                mViewModel.getPlayState().removeObservers(getActivity());
            }
        }
    }

    private static class PlayStateObserver implements Observer<Boolean> {
        private WeakReference<MediaHeadFragment> mFragmentWeak;

        private PlayStateObserver(MediaHeadFragment fragment) {
            this.mFragmentWeak = new WeakReference<>(fragment);
        }

        @Override
        public void onChanged(@Nullable Boolean aBoolean) {
            if (aBoolean != null && mFragmentWeak.get() != null) {
                mFragmentWeak.get().changeState(aBoolean);
            }
        }
    }
}
