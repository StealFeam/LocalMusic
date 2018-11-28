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
import com.zy.ppmusic.utils.Constant
import com.zy.ppmusic.utils.PrintLog
import com.zy.ppmusic.utils.SpUtils
import kotlinx.android.synthetic.main.activity_splash.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class SplashActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    private val requestCode = 0x010
    private val mPreferenceName = "SPLASH"
    private var animDuration:Long = 1500

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val attachTime = SpUtils.get().getOperator {
            it.getLong(Constant.SP_APP_ATTACH_TIME,System.currentTimeMillis())
        } as Long
        val diffTime = System.currentTimeMillis() - attachTime
        if(diffTime in 0..animDuration){
            animDuration = diffTime
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        App.setCustomDensity(this)
    }

    override fun onResume() {
        super.onResume()
        tv_splash_open.startAnimation(createLoadingAnim())
    }

    private fun createLoadingAnim(): Animation {
        println("动画时长：：：$animDuration")
        return AlphaAnimation(0.4f, 1f).apply {
            fillAfter = true
            duration = animDuration
            setAnimationListener(object : Animation.AnimationListener {
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
        if (tv_splash_open.animation != null) {
            tv_splash_open.animation.cancel()
            tv_splash_open.animation = null
        }
    }

}
