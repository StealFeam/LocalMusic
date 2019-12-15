package com.zy.ppmusic.widget

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.support.v4.media.session.MediaControllerCompat
import androidx.appcompat.app.AppCompatDialog
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
 * @author stealfeam
 * @date 2018/6/16
 */
class ChooseStyleDialog : androidx.fragment.app.DialogFragment(), IChooseNotifyStyleContract.IChooseNotifyStyleView {
    private val mPresenter: ChooseNotifyStylePresenter by lazy {
        ChooseNotifyStylePresenter(this)
    }

    private lateinit var mRadioGroup: RadioGroup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.dialog_choose_notify_style_layout, container)
        rootView ?: let {
            return super.onCreateView(inflater, container, savedInstanceState)!!
        }
        mRadioGroup = rootView.findViewById(R.id.rb_choose_parent)
        val localCheckId = mPresenter.getLocalStyle()
        val checkId = if (localCheckId >= 0) localCheckId else R.id.rb_choose_custom
        mRadioGroup.check(checkId)
        mRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            activity?.apply {
                val mediaController = MediaControllerCompat.getMediaController(requireActivity())
                mediaController?.let {
                    val extra = Bundle()
                    extra.putInt(Constant.CHOOSE_STYLE_EXTRA, checkedId)
                    mediaController.sendCommand(MediaService.COMMAND_CHANGE_NOTIFY_STYLE,
                            extra, null)
                }
            }
        }
        return rootView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AppCompatDialog(activity, R.style.NotifyDialogStyle)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        mPresenter.changeStyle(mRadioGroup.checkedRadioButtonId)
    }
}
