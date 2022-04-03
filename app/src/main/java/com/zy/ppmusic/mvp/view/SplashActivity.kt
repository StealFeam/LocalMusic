package com.zy.ppmusic.mvp.view

import android.app.Activity
import android.content.res.Resources
import android.os.Bundle
import android.widget.TextView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.zy.ppmusic.App
import com.zy.ppmusic.R

class SplashActivity : Activity(){
    private var splashTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        App.setCustomDensity(this)
        splashTextView = findViewById(R.id.splashTextView)
        actionToMain()
    }

    override fun getResources(): Resources = App.instance!!.resources

    private fun actionToMain() {
        MediaActivity.action(this)
        finish()
    }
}
