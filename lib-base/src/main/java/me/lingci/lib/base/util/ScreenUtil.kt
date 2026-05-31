package me.lingci.lib.base.util

import android.content.res.Configuration

/**
 *   @author : happyc
 *   time    : 2024/09/19
 *   desc    :
 *   version : 1.0
 */
object ScreenUtil {

    @JvmStatic
    fun isTablet(configuration: Configuration): Boolean {
        // 检查是否支持大屏幕（平板等大尺寸设备）
        return configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    @JvmStatic
    fun isXLargeTablet(configuration: Configuration): Boolean {
        // 检查是否是特大屏幕设备（例如平板）
        return configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }

}