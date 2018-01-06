package com.zy.ppmusic.base;

import java.lang.ref.WeakReference;

/**
 * @author lengs
 */
public interface IBasePresenter {
    /**
     * view解绑操作
     */
    void destroyView();
}
