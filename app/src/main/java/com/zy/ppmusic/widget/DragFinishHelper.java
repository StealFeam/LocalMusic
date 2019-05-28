package com.zy.ppmusic.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import androidx.annotation.Keep;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import java.lang.ref.WeakReference;

/**
 * @author stealfeam
 */
class DragFinishHelper {
    private static final String TAG = "DragFinishHelper";
    private WeakReference<ViewGroup> mWeakRootView;
    private DragFinishListener listener;
    private int minTranslateX = 0;
    private float minXVelocity;
    private VelocityTracker mXVelocity;
    private boolean isDrag;
    private int rawX = 0;
    private int rawY = 0;

    public void attachView(ViewGroup root) {
        mWeakRootView = new WeakReference<>(root);
        if (root != null) {
            ViewConfiguration viewConfiguration = ViewConfiguration.get(root.getContext());
            minTranslateX = viewConfiguration.getScaledTouchSlop();
            minXVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
            Log.e(TAG, "attachView: min=" + minTranslateX);
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
                if (mXVelocity == null) {
                    mXVelocity = VelocityTracker.obtain();
                } else {
                    mXVelocity.clear();
                }
                mXVelocity.addMovement(event);
                return false;
//                if (rawX <= minTranslateX) {
//                    isDrag = true;
//                    mXVelocity.addMovement(event);
//                    return true;
//                } else {
//                    isDrag = false;
//                    return false;
//                }
            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) (event.getRawX() - rawX);
                int deltaY = (int) (event.getRawY() - rawY);
                mXVelocity.addMovement(event);
                mXVelocity.computeCurrentVelocity(1000);
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
                        mXVelocity.computeCurrentVelocity(1000);
                        int xRatio = 22;
                        if (mXVelocity.getXVelocity() > (minXVelocity * xRatio)) {
                            scrollTo(root.getRootView().getMeasuredWidth());
                            mXVelocity.recycle();
                            return true;
                        }
                        int equal = 3;
                        if (root.getRootView().getTranslationX() < root.getRootView().getMeasuredWidth() / equal) {
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
            ObjectAnimator animator = ObjectAnimator.ofFloat(root.getRootView(), View.TRANSLATION_X,
                    root.getRootView().getTranslationX(), 0);
            animator.setDuration(300);
            animator.setInterpolator(new LinearInterpolator());
            animator.start();
        }
    }

    private void scrollTo(int deltaX) {
        if (mWeakRootView != null) {
            final ViewGroup root = mWeakRootView.get();
            ObjectAnimator animator = ObjectAnimator.ofFloat(root.getRootView(), View.TRANSLATION_X,
                    root.getRootView().getTranslationX(), deltaX);
            animator.setDuration(400);
            animator.setInterpolator(new LinearInterpolator());
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    root.clearAnimation();
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
    @Keep
    public interface DragFinishListener {
        /**
         * 滑动到右边界
         */
        void scrollToRightBorder();
    }
}
