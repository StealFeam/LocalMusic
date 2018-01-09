package com.zy.ppmusic.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import com.zy.ppmusic.R
import com.zy.ppmusic.mvp.view.MediaActivity
import com.zy.ppmusic.widget.PermissionDialog
import kotlinx.android.synthetic.main.activity_splash.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class SplashActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    val REQUEST_CODE = 0x010
    private val PREFERENCE_NAME = "SPLASH"
    var permissionDesDialog:PermissionDialog?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val animation = AnimationSet(true)
        val alphaAnim = AlphaAnimation(0f, 1f)
        alphaAnim.fillAfter = true
        alphaAnim.duration = 1500
        animation.addAnimation(alphaAnim)

        tv_splash_open.animation = animation
        animation.start()
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(a: Animation?) {
            }

            override fun onAnimationStart(a: Animation?) {
            }

            override fun onAnimationEnd(a: Animation?) =
                    if (EasyPermissions.hasPermissions(applicationContext, getString(R.string.string_read_external)
                            , getString(R.string.string_write_external))) {
                        actionToMain()
                    } else {
                        val builder = PermissionDialog.Builder()
                        val simpleList = ArrayList<String>()
                        simpleList.add("访问本地文件")
                        builder.setPermissions(simpleList)
                        builder.setTitle("我正常启动需要以下权限")
                        builder.setSureTitle("知道了")
                        permissionDesDialog = builder.build()
                        permissionDesDialog!!.show(this@SplashActivity,listener)
                    }
        })


    }

    private val listener = View.OnClickListener {
        EasyPermissions.requestPermissions(this@SplashActivity, getString(R.string.string_permission_read)
                , REQUEST_CODE, getString(R.string.string_read_external)
                , getString(R.string.string_write_external))
    }


    fun actionToMain() {
        val mediaIntent = Intent(this, MediaActivity::class.java)
        startActivity(mediaIntent)
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>?) {
        if (EasyPermissions.hasPermissions(applicationContext, getString(R.string.string_read_external)
                , getString(R.string.string_write_external))) {
            actionToMain()
            return
        }

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms!!)) {
            val dialog = AppSettingsDialog.Builder(this)
            dialog.setNegativeButton(R.string.exit)
            dialog.build().show()
        } else {
            val preference = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            val isInit = preference.getBoolean("isInitPermission", false)
            if (!isInit) {
                EasyPermissions.requestPermissions(this, getString(R.string.string_permission_read)
                        , REQUEST_CODE, getString(R.string.string_read_external)
                        , getString(R.string.string_write_external))
                preference.edit().putBoolean("isInitPermission", true).apply()
            } else {
                finish()
                System.exit(0)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            if (resultCode == -1) {
                finish()
                System.exit(-1)
            } else {
                if (EasyPermissions.hasPermissions(applicationContext, getString(R.string.string_read_external)
                        , getString(R.string.string_write_external))) {
                    actionToMain()
                } else {
                    EasyPermissions.requestPermissions(this, getString(R.string.string_permission_read)
                            , REQUEST_CODE, getString(R.string.string_read_external)
                            , getString(R.string.string_write_external))
                }
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>?) {
        actionToMain()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(permissionDesDialog != null){
            permissionDesDialog!!.cancelDialog()
        }
    }

}
