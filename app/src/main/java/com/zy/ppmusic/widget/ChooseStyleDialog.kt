package com.zy.ppmusic.widget

import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v7.app.AppCompatDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import com.zy.ppmusic.R
import com.zy.ppmusic.mvp.contract.IChooseNotifyStyleContract
import com.zy.ppmusic.mvp.presenter.ChooseNotifyStylePresenter
import com.zy.ppmusic.service.MediaService
import com.zy.ppmusic.utils.Constant

/**
 * @author y-slience
 * @date 2018/6/16
 */
class ChooseStyleDialog : DialogFragment(), IChooseNotifyStyleContract.IChooseNotifyStyleView {
    private val mPresenter: ChooseNotifyStylePresenter by lazy {
        ChooseNotifyStylePresenter(this)
    }

    private lateinit var mRadioGroup: RadioGroup

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater?.inflate(R.layout.dialog_choose_notify_style_layout, container)
        rootView ?: let {
            return super.onCreateView(inflater, container, savedInstanceState)
        }
        mRadioGroup = rootView.findViewById(R.id.rb_choose_parent)
        val localCheckId = mPresenter.getLocalStyle()
        val checkId = if (localCheckId >= 0)
            localCheckId else
            R.id.rb_choose_custom
        try {
            mRadioGroup.check(checkId)
            mRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                val mediaController = MediaControllerCompat.getMediaController(activity)
                val extra = Bundle()
                extra.putInt(Constant.CHOOSE_STYLE_EXTRA,checkedId)
                mediaController.sendCommand(MediaService.COMMAND_CHANGE_NOTIFY_STYLE,
                        extra,null)
            }
        } catch (e: Exception) {
            EasyTintView.makeText(rootView, "出现了错误，尴尬", EasyTintView.TINT_SHORT).show()
        }

        return rootView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AppCompatDialog(activity,R.style.NotifyDialogStyle)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        mPresenter.changeStyle(mRadioGroup.checkedRadioButtonId)
    }
}