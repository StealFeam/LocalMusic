package com.zy.ppmusic.mvp.view

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import com.zy.ppmusic.App
import com.zy.ppmusic.R
import com.zy.ppmusic.utils.PrintLog
import kotlinx.android.synthetic.main.activity_splash.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class SplashActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    private val requestCode = 0x010
    private val mPreferenceName = "SPLASH"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        App.setCustomDensity(this)
        App.getInstance().createActivity(this)
    }

    override fun onResume() {
        super.onResume()
        tv_splash_open.startAnimation(createLoadingAnim())
    }

    private fun createLoadingAnim(): Animation {
        return AlphaAnimation(0.4f, 1f).apply {
            this.fillAfter = true
            this.duration = 1500
            this.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(a: Animation?) = PrintLog.e("onAnimationRepeat....")

                override fun onAnimationStart(a: Animation?) = PrintLog.e("onAnimationStart....")

                override fun onAnimationEnd(a: Animation?) {
                    PrintLog.e("onAnimationEnd......")
                    if (EasyPermissions.hasPermissions(applicationContext, getString(R.string.string_read_external)
                                    , getString(R.string.string_write_external))) {
                        actionToMain()
                    } else {
                        EasyPermissions.requestPermissions(this@SplashActivity,
                                getString(R.string.string_permission_read), requestCode,
                                getString(R.string.string_read_external)
                                , getString(R.string.string_write_external))
                    }
                }
            })
        }
    }

    override fun getResources(): Resources = App.getInstance().resources

    fun actionToMain() {
        MediaActivity.action(this)
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
            val preference = getSharedPreferences(mPreferenceName, Context.MODE_PRIVATE)
            val isInit = preference.getBoolean("isInitPermission", false)
            if (!isInit) {
                EasyPermissions.requestPermissions(this, getString(R.string.string_permission_read)
                        , this.requestCode, getString(R.string.string_read_external)
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
                            , this.requestCode, getString(R.string.string_read_external)
                            , getString(R.string.string_write_external))
                }
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>?) = actionToMain()

    override fun onStop() {
        super.onStop()
        if (tv_splash_open.animation != null) {
            tv_splash_open.animation.cancel()
            tv_splash_open.animation = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        App.getInstance().destroyActivity(this)
        if (tv_splash_open.animation != null) {
            tv_splash_open.animation.cancel()
            tv_splash_open.animation = null
        }
    }

}
