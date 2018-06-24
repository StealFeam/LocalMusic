package com.zy.ppmusic.mvp.contract

import com.zy.ppmusic.mvp.base.AbstractBasePresenter
import com.zy.ppmusic.mvp.base.IBaseModel
import com.zy.ppmusic.mvp.base.IBaseView

/**
 * @author y-slience
 * @date 2018/6/16
 */
interface IChooseNotifyStyleContract {
    interface IChooseNotifyStyleView :IBaseView{
    }

    interface IChooseNotifyStyleModel : IBaseModel{
        fun changeStyle(styleId:Int)

        fun getLocalStyle():Int
    }

    abstract class AbsChooseNotifyStylePresenter(view:IChooseNotifyStyleView):AbstractBasePresenter<IChooseNotifyStyleView,
            IChooseNotifyStyleModel>(view){
        abstract fun changeStyle(styleId:Int)

        abstract fun getLocalStyle():Int
    }
}