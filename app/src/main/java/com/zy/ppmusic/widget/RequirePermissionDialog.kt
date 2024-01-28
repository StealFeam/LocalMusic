package com.zy.ppmusic.widget

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface.OnDismissListener
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.zy.ppmusic.service.primaryColor

class RequirePermissionDialog: DialogFragment() {

    var dialogDismissListener: OnDismissListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply{
            setContent {
                val context = LocalContext.current
                Snackbar(
                    modifier = Modifier.fillMaxWidth(),
                    action = {
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()){
                            Button(
                                onClick = {
                                    // Create app settings intent
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    val uri = Uri.fromParts("package", context.packageName, null)
                                    intent.setData(uri)
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent, contentColor = Color.Transparent),
                                elevation = null,
                            ) {
                                Text(
                                    text = "去设置",
                                    color = primaryColor
                                )
                            }
                        }
                    }
                ) {
                    Text(text = "需要存储权限")
                }
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.run {
            setCanceledOnTouchOutside(false)
            window?.setDimAmount(0.1f)
            window?.setGravity(Gravity.BOTTOM)
            setOnDismissListener(dialogDismissListener)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                dismiss()
            }
        } else {
            val checkResult = arrayOf(
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE),
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
            if (checkResult.none { it != PackageManager.PERMISSION_GRANTED }) {
                dismiss()
            }
        }
    }
}