package me.lingci.dy.player

import android.app.Application
import android.content.Context
import com.tencent.bugly.crashreport.CrashReport
import me.lingci.dy.player.cache.ProgressManagerImpl
import me.lingci.lib.base.crash.AppExceptionHandler
import me.lingci.lib.player.exo.CustomExoMediaPlayerFactory
import xyz.doikki.videoplayer.player.VideoViewConfig
import xyz.doikki.videoplayer.player.VideoViewManager

/**
 * @author : happyc
 * time    : 2020/06/28
 * desc    :
 * version : 1.0
 */
internal class PlayerApp : Application() {

    companion object {

        private var appContext: Context? = null

        fun getAppContext(): Context {
            return appContext!!
        }

    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        initDkPlayer()
        initCrash()
    }

    private fun initDkPlayer() {
        VideoViewManager.setConfig(
            VideoViewConfig.newBuilder()
                // Global default is only a safe fallback for views without explicit core injection.
                // dy-player playback screens override each CustomVideoView via DyPlayerCoreRegistry.
                .setPlayerFactory(CustomExoMediaPlayerFactory.create())
                //.setRenderViewFactory(SurfaceRenderViewFactory.create())
                .setProgressManager(ProgressManagerImpl())
                .setLogEnabled(BuildConfig.DEBUG)
                .build()
        )
    }

    private fun initCrash() {
        val buglyAppId = BuildConfig.BUGLY_APP_ID
        if (buglyAppId.isNotEmpty()) {
            CrashReport.initCrashReport(applicationContext, buglyAppId, false)
        }
        Thread.setDefaultUncaughtExceptionHandler(AppExceptionHandler(this))
    }

}
