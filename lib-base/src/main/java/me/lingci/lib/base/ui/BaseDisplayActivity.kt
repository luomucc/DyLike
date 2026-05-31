package me.lingci.lib.base.ui

import android.content.Context
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity

/**
 *   @author : happyc
 *   time    : 2025/07/08
 *   desc    : https://www.jianshu.com/p/c4944ea4b85f 改变字体缩放，让应用保持字体大小
 *   version : 1.0
 */
abstract class BaseDisplayActivity: AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        //super.attachBaseContext(newBase)
        val configuration = newBase.resources.configuration
        configuration.fontScale = 1f
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun getResources(): Resources {
        //return super.getResources()
        val resources = super.getResources()
        val configuration = resources.configuration
        if (configuration.fontScale != 1f) {
            configuration.fontScale = 1f
            return baseContext.createConfigurationContext(configuration).resources
        } else {
            return resources
        }
    }

}