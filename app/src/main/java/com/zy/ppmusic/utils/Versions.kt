package com.zy.ppmusic.utils

import android.os.Build

val currentOsSDK: Int = Build.VERSION.SDK_INT

/**
 * 低于S版本
 */
val belowSVersion: Boolean = currentOsSDK < Build.VERSION_CODES.S
