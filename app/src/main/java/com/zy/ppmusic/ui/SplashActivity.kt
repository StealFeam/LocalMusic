package com.zy.ppmusic.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.widget.TextView
import com.zy.ppmusic.R
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class SplashActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    var REQUEST_CODE = 0x010
    private var PREFERENCE_NAME = "SPLASH"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val tvSplash = findViewById(R.id.tv_splash_open) as TextView
        val animation = AnimationSet(true)
        val alphaAnim = AlphaAnimation(0f, 1f)
        alphaAnim.fillAfter = true
        alphaAnim.duration = 1500
        animation.addAnimation(alphaAnim)

        tvSplash.animation = animation
        animation.start()
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                if (EasyPermissions.hasPermissions(applicationContext, getString(R.string.string_read_external)
                        , getString(R.string.string_write_external))) {
                    actionToMain()
                } else {
                    EasyPermissions.requestPermissions(this@SplashActivity, getString(R.string.string_permission_read)
                            , REQUEST_CODE, getString(R.string.string_read_external)
                            , getString(R.string.string_write_external))
                }
            }
        })


    }


    fun actionToMain() {
        val mediaIntent = Intent(this@SplashActivity, MediaActivity::class.java)
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
}
