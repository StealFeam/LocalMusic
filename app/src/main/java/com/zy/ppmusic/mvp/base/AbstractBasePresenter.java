package com.zy.ppmusic.mvp.base;

import java.lang.ref.WeakReference;

/**
 * @author stealfeam
 * @date 2018/1/12
 */

public abstract class AbstractBasePresenter<V extends IBaseView,M extends IBaseModel> {
    protected WeakReference<V> mView;
    protected M mModel;

    public AbstractBasePresenter(V v) {
        this.mView = new WeakReference<>(v);
        this.mModel = createModel();
    }

    /**
     * 创建model实例
     * @return 实例对象
     */
    protected abstract M createModel();

    /**
     * 销毁View和Model的引用
     */
    public void detachViewAndModel(){
        if (mView != null) {
            mView.clear();
        }
    }
}
