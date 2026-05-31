package me.lingci.lib.base.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import java.util.Locale

/**
 * Created by jinliangshan on 16/12/26.
 */
object AccessibilityUtil {

    @JvmStatic
    fun checkAccessibility(context: Context, message: String): Boolean {
        // 判断辅助功能是否开启
        if (isAccessibilitySettingsOn(context)) {
            return true
        }
        // 引导至辅助功能设置页面
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        ToastUtil.showToast(context, message)
        return false
    }

    @JvmStatic
    fun isAccessibilitySettingsOn(context: Context): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (services != null) {
                return services.lowercase(Locale.getDefault()).contains(
                    context.packageName.lowercase(
                        Locale.getDefault()
                    )
                )
            }
        }
        return false
    }
}