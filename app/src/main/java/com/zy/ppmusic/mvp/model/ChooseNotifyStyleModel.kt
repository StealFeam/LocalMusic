package com.zy.ppmusic.mvp.model

import android.content.Context
import android.content.SharedPreferences
import com.zy.ppmusic.App
import com.zy.ppmusic.mvp.contract.IChooseNotifyStyleContract
import com.zy.ppmusic.utils.Constant

/**
 * @author stealfeam
 * @date 2018/6/16
 */
class ChooseNotifyStyleModel : IChooseNotifyStyleContract.IChooseNotifyStyleModel {
    private val localSharePreference: SharedPreferences by lazy {
        App.instance.getSharedPreferences(Constant.LOCAL_CHOOSE_FILE, Context.MODE_PRIVATE)
    }
    private var localId = 0

    override fun changeStyle(styleId: Int) {
        if(localId == 0){
            localId = getLocalStyle()
        }
        if (styleId == localId) {
            return
        }
        localSharePreference.edit().putInt(Constant.LOCAL_STYLE_NAME,styleId).apply()
        localId = styleId
    }

    override fun getLocalStyle(): Int {
        return localSharePreference.getInt(Constant.LOCAL_STYLE_NAME,-1)
    }
}