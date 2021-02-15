package com.zy.ppmusic.utils

import android.os.Environment
import com.zy.ppmusic.App

/**
 * @author stealfeam
 * @date 2018/6/11
 */
object Constant {
    var IS_STARTED = false
    //本地存储通知栏样式选择
    const val LOCAL_CHOOSE_FILE = "LOCAL_CHOOSE_STYLE"
    const val CHOOSE_STYLE_EXTRA = "CHOOSE_STYLE_EXTRA"
    const val LOCAL_STYLE_NAME = "LOCAL_STYLE_ID"
    val CACHE_FILE_PATH = "${App.instance!!.getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath}/pathlist.obj"
    const val SP_APP_ATTACH_TIME = "app_attach_time"

    val FILTER_DIRS = listOf(
        "cache",
        "Cache",
        "alibaba",
        "alipay",
        "Alipay",
        "emojiFiles",
        "SDK",
        "sdk",
        "youku",
        "log",
        "pindd",
        "baojia",
        "system",
        "Youdao",
        "zhaopin",
        "plugin",
        "WifiMasterKey",
        "gifshow",
        "hawaii",
        "BaiduMap",
        "setup",
        "libs",
        "userdata",
        "tmp",
        "game",
        "com_tencent_tmgp_sgame",
        "detail",
        "imagefilter",
        "video_play_manifest_cache",
        "photoalbum_emotion",
        " Images",
        "crash",
        "Mob",
        "icbcim",
        "trafficLogic",
        "applogic",
        "pcdn",
        "LogStats",
        "LocalCache",
        "lianjia",
        "autohomemain",
        "lagou",
        "wifimanager",
        "photoalbum_emotion",
        "HttpDnsCache",
        "hotfix",
        "MicroMsg",
        "QQ_Favorite",
        "QQ_Images",
        "tbs",
        "backup",
        "information_paster",
        "chatthumb",
        "chatpic",
        "doodle_template",
        "sv_config_resource",
        "pddata",
        "msflogs",
        "amap",
        "wireless",
        "dov_doodle_music",
        "RedPacket",
        "thumb",
        "capture_ptv_template"
    )
}
