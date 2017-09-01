package com.zy.ppmusic.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import com.zy.ppmusic.R;

public class EasyTintView extends AppCompatTextView {
    private static final String TAG = "EasyTintView";
    public static final int TINT_LONG = 3000;
    public static final int TINT_SHORT = 1000;
    private Handler mDelayHandler;
    private int delayDuration;
    private int showGravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
    private boolean isVisible = false;
    private ViewGroup parent;

    public EasyTintView(Context context) {
        super(context);
        mDelayHandler = new Handler();
        setPadding(20, 10, 20, 10);
        setBackgroundResource(R.drawable.normal_tint_shape);
        getBackground().setAlpha(130);
        setTextColor(Color.WHITE);
        setTag(TAG);
    }

    public static EasyTintView makeText(View anchorView, String msg, int duration) {
        ViewGroup parent = findSuitableParent(anchorView);
        View tagView = null;
        if (parent != null) {
            tagView = parent.findViewWithTag(TAG);
        }
        EasyTintView tintView;
        if (tagView != null) {
            tintView = (EasyTintView) tagView;
        } else {
            tintView = new EasyTintView(anchorView.getContext());
        }
        tintView.setText(msg);
        tintView.parent = parent;
        tintView.delayDuration = duration;
        return tintView;
    }

    private void setShowGravity(int gravity) {
        this.showGravity = gravity;
    }

    public void show() {
        if (isVisible) {
            mDelayHandler.removeCallbacksAndMessages(null);
            mDelayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideAnim();
                }
            }, delayDuration);
        } else {
            showAnim();
        }
    }

    private void showAnim() {
        if (parent == null) {
            Log.e(TAG, "showAnim: parent is null");
            return;
        }
        if (isVisible) {
            System.out.println("view is already showing in screen");
            return;
        }


        if (this.getParent() == null) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = showGravity;
            params.topMargin = 5;
            parent.addView(this, params);
        } else {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) this.getLayoutParams();
            params.gravity = showGravity;
            params.topMargin = 5;
            this.setLayoutParams(params);
        }

        Animation showAnim = AnimationUtils.loadAnimation(getContext(), R.anim.tint_show_anim);
        showAnim.setDuration(200);
        showAnim.setFillAfter(true);
        this.setAnimation(showAnim);
        showAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isVisible = true;
                mDelayHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hideAnim();
                    }
                }, delayDuration);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        showAnim.start();
    }

    private void hideAnim() {
        if (isVisible) {
            removeFromParent();
            Animation hideAnim = AnimationUtils.loadAnimation(getContext(), R.anim.tint_hide_anim);
            hideAnim.setDuration(200);
            hideAnim.setFillAfter(true);
            this.setAnimation(hideAnim);
            hideAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    isVisible = false;
                    mDelayHandler.removeCallbacksAndMessages(null);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            hideAnim.start();
        }

    }

    private void removeFromParent() {
        if (parent != null && this.getParent() != null) {
            parent.removeView(this);
        }
    }

    private static ViewGroup findSuitableParent(View view) {
        do {
            if (view instanceof FrameLayout) {
                if (view.getId() == android.R.id.content) {
                    return (ViewGroup) view;
                }
            }
            if (view != null) {
                ViewParent parent = view.getParent();
                view = parent instanceof View ? (View) parent : null;
            }
        } while (view != null);

        return null;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mDelayHandler.removeCallbacksAndMessages(null);
    }

}
