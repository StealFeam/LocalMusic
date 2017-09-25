package com.zy.ppmusic.view;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import java.lang.ref.WeakReference;

public class DragFinishHelper {
    private static final String TAG = "DragFinishHelper";
    private WeakReference<ViewGroup> mWeakRootView;
    private DragFinishListener listener;
    private int minTranslateX = 0;
    private boolean isDrag;
    private int rawX = 0;
    private int rawY = 0;

    public void attachView(ViewGroup root) {
        mWeakRootView = new WeakReference<ViewGroup>(root);
        if (root != null) {
            ViewConfiguration viewConfiguration = ViewConfiguration.get(root.getContext());
            minTranslateX = viewConfiguration.getScaledTouchSlop();
        }
    }

    public void setListener(DragFinishListener l) {
        this.listener = l;
    }

    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent() called with: event = [" + event + "]");
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                rawX = (int) event.getRawX();
                rawY = (int) event.getRawY();
                if(rawX <= 10){
                    isDrag = true;
                    return true;
                }else{
                    isDrag = false;
                    return false;
                }
            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) (event.getRawX() - rawX);
                int deltaY = (int) (event.getRawY() - rawY);
                if (Math.abs(deltaX) > minTranslateX) {
                    if (Math.abs(deltaX) > Math.abs(deltaY) && deltaX > 0) {
                        if (mWeakRootView != null) {
                            ViewGroup root = mWeakRootView.get();
                            root.getRootView().setTranslationX(deltaX);
                        }
                        isDrag = true;
                        return true;
                    }
                }
                isDrag = false;
                return false;
            case MotionEvent.ACTION_UP:
                if (mWeakRootView != null) {
                    ViewGroup root = mWeakRootView.get();
                    if (isDrag) {
                        if (root.getRootView().getTranslationX() < root.getRootView().getMeasuredWidth() / 3) {
                            scrollToStart();
                        } else {
                            scrollTo(root.getRootView().getMeasuredWidth());
                            return true;
                        }
                    } else {
                        root.getFocusedChild().performClick();
                    }
                }
                return false;
            default:
                return false;
        }
    }

    private void scrollToStart() {
        if (mWeakRootView != null) {
            ViewGroup root = mWeakRootView.get();
            ObjectAnimator animator;
            animator = ObjectAnimator.ofFloat(root.getRootView(), "translationX",
                    root.getRootView().getTranslationX(), 0);
            animator.setDuration(300);
            animator.setInterpolator(new LinearInterpolator());
            animator.start();
        }
    }

    private void scrollTo(int deltaX) {
        if (mWeakRootView != null) {
            ViewGroup root = mWeakRootView.get();
            ObjectAnimator animator;
            animator = ObjectAnimator.ofFloat(root.getRootView(), "translationX",
                    root.getRootView().getTranslationX(), deltaX);
            animator.setDuration(400);
            animator.setInterpolator(new LinearInterpolator());
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (listener != null) {
                        listener.scrollToRightBorder();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.start();
        }
    }

    public interface DragFinishListener {
        void scrollToRightBorder();
    }
}
