package com.zy.ppmusic.mvp.view

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v7.app.AppCompatActivity
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.animation.BounceInterpolator
import com.zy.ppmusic.App
import com.zy.ppmusic.R
import com.zy.ppmusic.utils.PrintLog
import kotlinx.android.synthetic.main.activity_splash.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class SplashActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    private val requestCode = 0x010
    private val mPreferenceName = "SPLASH"
    private val mStartSet = ConstraintSet()
    private val mEndSet = ConstraintSet()
    private lateinit var mRootLayout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        App.getInstance().createActivity(this)
        mRootLayout = findViewById(R.id.root_splash)
        mEndSet.clone(this, R.layout.activity_splash_end)
        mStartSet.clone(mRootLayout)
    }

    override fun onResume() {
        super.onResume()
        mRootLayout.post({
            startTransition()
        })
    }

    private fun startTransition() {
        val changeBounds = ChangeBounds()
        changeBounds.duration = 1200
        changeBounds.interpolator = BounceInterpolator()
        changeBounds.addListener(object : android.transition.Transition.TransitionListener {
            override fun onTransitionEnd(transition: android.transition.Transition?) {
                PrintLog.e("onTransitionEnd....")
                transition?.removeListener(this)
                transition?.removeTarget(mRootLayout)
                actionToMain()
            }

            override fun onTransitionResume(transition: android.transition.Transition?) {
                PrintLog.e("onTransitionResume....")
            }

            override fun onTransitionPause(transition: android.transition.Transition?) {
                PrintLog.e("onTransitionPause....")
            }

            override fun onTransitionCancel(transition: android.transition.Transition?) {
                PrintLog.e("onTransitionCancel....")
                transition?.removeListener(this)
                transition?.removeTarget(mRootLayout)
                actionToMain()
            }

            override fun onTransitionStart(transition: android.transition.Transition?) {
                PrintLog.e("startTransition....")
            }

        })

        TransitionManager.beginDelayedTransition(mRootLayout, changeBounds)
        mEndSet.applyTo(mRootLayout)
    }

    override fun getResources(): Resources = App.getInstance().resources

    private fun actionToMain() {
        if (EasyPermissions.hasPermissions(applicationContext, getString(R.string.string_read_external)
                        , getString(R.string.string_write_external))) {
            val mediaIntent = Intent(this, MediaActivity::class.java)
            startActivity(mediaIntent)
            finish()
        } else {
            EasyPermissions.requestPermissions(this@SplashActivity,
                    getString(R.string.string_permission_read), requestCode,
                    getString(R.string.string_read_external)
                    , getString(R.string.string_write_external))
        }
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

    override fun onDestroy() {
        super.onDestroy()
        App.getInstance().destroyActivity(this)

    }

}
