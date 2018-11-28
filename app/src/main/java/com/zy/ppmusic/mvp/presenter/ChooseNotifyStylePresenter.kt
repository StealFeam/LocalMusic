package com.zy.ppmusic.mvp.presenter

import com.zy.ppmusic.mvp.contract.IChooseNotifyStyleContract
import com.zy.ppmusic.mvp.model.ChooseNotifyStyleModel

/**
 * @author stealfeam
 * @date 2018/6/16
 */
class ChooseNotifyStylePresenter(view:IChooseNotifyStyleContract.IChooseNotifyStyleView):
        IChooseNotifyStyleContract.AbsChooseNotifyStylePresenter(view) {

    override fun changeStyle(styleId: Int) {
        mModel.changeStyle(styleId)
    }

    override fun getLocalStyle(): Int {
        return mModel.getLocalStyle()
    }

    override fun createModel(): IChooseNotifyStyleContract.IChooseNotifyStyleModel  = ChooseNotifyStyleModel()
}