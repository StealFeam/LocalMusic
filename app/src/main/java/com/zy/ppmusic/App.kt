package com.zy.ppmusic

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.multidex.MultiDexApplication
import com.zy.ppmusic.data.db.DataBaseManager
import com.zy.ppmusic.utils.Constant
import com.zy.ppmusic.utils.CrashHandler
import com.zy.ppmusic.utils.SpUtils.Companion.get

/**
 * @author stealfeam
 */
class App : MultiDexApplication() {

    var databaseManager: DataBaseManager? = null
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        get().putOperator { editor ->
            editor.putLong(Constant.SP_APP_ATTACH_TIME, System.currentTimeMillis())
        }
        val handler = CrashHandler(this)
        if (!BuildConfig.IS_DEBUG) handler.attach()
        databaseManager = DataBaseManager.getInstance().initDb(this)
    }

    val context: Context
        get() = this.applicationContext

    companion object {
        var instance: App? = null

        @JvmStatic fun setCustomDensity(activity: Activity) {
            val application: Application? = instance
            val appDisplayMetrics = application!!.resources.displayMetrics

            //px = density * dp
            //density= dpi / 160
            //px = dp * (dpi / 160)
            val targetDensity = appDisplayMetrics.widthPixels / 360.toFloat()
            val targetDensityDpi = (160 * targetDensity).toInt()
            appDisplayMetrics.density = targetDensity
            appDisplayMetrics.densityDpi = targetDensityDpi
            appDisplayMetrics.scaledDensity = targetDensity
            val activityDisplayMetrics = activity.resources.displayMetrics
            activityDisplayMetrics.scaledDensity = targetDensity
            activityDisplayMetrics.density = activityDisplayMetrics.scaledDensity
            activityDisplayMetrics.densityDpi = targetDensityDpi
        }

        @JvmStatic val appBaseContext: Context get() = instance!!.baseContext
    }
}
