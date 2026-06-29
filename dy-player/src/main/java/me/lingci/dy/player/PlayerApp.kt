package me.lingci.dy.player

import android.app.Application
import android.content.Context
import com.tencent.bugly.crashreport.CrashReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.lingci.dy.player.cache.ProgressManagerImpl
import me.lingci.dy.player.util.SpUtil
import me.lingci.lib.base.crash.AppExceptionHandler
import me.lingci.lib.base.json.JSONObject
import me.lingci.lib.base.util.CodeUtil
import me.lingci.lib.base.util.FileOperator
import me.lingci.lib.base.util.Log
import me.lingci.lib.player.exo.CustomExoMediaPlayerFactory
import xyz.doikki.videoplayer.player.VideoViewConfig
import xyz.doikki.videoplayer.player.VideoViewManager
import java.io.File

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
        checkAndAutoBackup()
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 检测版本升级并自动备份设置
     */
    private fun checkAndAutoBackup() {
        appScope.launch {
            try {
                val spUtil = SpUtil(applicationContext)
                val currentVersionCode = CodeUtil.versionCode(applicationContext).toInt()
                val lastVersionCode = spUtil.lastVersionCode

                if (lastVersionCode > 0 && currentVersionCode != lastVersionCode) {
                    // 检测到版本升级，自动备份当前设置
                    Log.d("PlayerApp", "检测到版本升级: $lastVersionCode -> $currentVersionCode，开始自动备份")
                    autoBackupSettings(spUtil, currentVersionCode)
                } else if (lastVersionCode == 0) {
                    // 首次安装，记录版本号
                    spUtil.lastVersionCode = currentVersionCode
                }
            } catch (e: Exception) {
                Log.d("PlayerApp", "自动备份失败: ${e.message}", e)
            }
        }
    }

    /**
     * 自动备份设置到 /Download/DyLike/auto_backup_v{version}.json
     */
    private fun autoBackupSettings(spUtil: SpUtil, versionCode: Int) {
        try {
            val data = JSONObject()
            data.putValue("backupType", "dy_like_auto")
            data.putValue("backupVersion", versionCode)
            data.putValue("dataSchemaVersion", spUtil.dataSchemaVersion)
            data.putValue("isFirst", spUtil.isFirst)
            data.putValue("sourceJson", spUtil.sourceJson!!)
            data.putValue("mediaJson", spUtil.mediaJson!!)
            data.putValue("historyJson", spUtil.historyJson!!)
            data.putValue("likeJson", spUtil.likeJson!!)
            data.putValue("playlistJson", spUtil.playlistJson!!)
            data.putValue("dayStr", spUtil.dayStr!!)
            data.putValue("videoDetailMode", spUtil.videoDetailMode)
            data.putValue("videoPlayerCore", spUtil.videoPlayerCore)
            data.putValue("shortVideoPlayerCore", spUtil.shortVideoPlayerCore)
            data.putValue("longVideoMode", spUtil.longVideoMode)
            data.putValue("shortRandom", spUtil.shortRandom)
            data.putValue("dmGradientMode", spUtil.dmGradientMode)
            data.putValue("dmGradientRatio", spUtil.dmGradientRatio)
            data.putValue("dmGradientWithTextColor", spUtil.dmGradientWithTextColor)
            data.putValue("dmStrokeMultipleMode", spUtil.dmStrokeMultipleMode)
            data.putValue("dmStrokeMultiple", spUtil.dmStrokeMultiple)
            data.putValue("dmFontMode", spUtil.dmFontMode)
            data.putValue("dmCurrentFont", spUtil.dmCurrentFont!!)
            data.putValue("firstScanMovie", spUtil.firstScanMovie)
            data.putValue("surfaceRender", spUtil.surfaceRender)
            data.putValue("dmMergeMode", spUtil.dmMergeMode)
            data.putValue("dmShowTime", spUtil.dmShowTime)
            data.putValue("dmMergeTop", spUtil.dmMergeTop)
            data.putValue("dmMergeShow", spUtil.dmMergeShow)
            data.putValue("browserUsedAll", spUtil.browserUsedAll)
            data.putValue("browserSort", spUtil.browserSort)
            data.putValue("browserShowHide", spUtil.browserShowHide)
            data.putValue("newHome", spUtil.newHome)
            data.putValue("sortRender", spUtil.sortRender)
            data.putValue("useOkhttp", spUtil.useOkhttp)
            data.putValue("autoNext", spUtil.autoNext)
            data.putValue("loopList", spUtil.loopList)
            data.putValue("showShortTitle", spUtil.showShortTitle)
            data.putValue("showShortLike", spUtil.showShortLike)
            data.putValue("showShortComment", spUtil.showShortComment)
            data.putValue("showShortPager", spUtil.showShortPager)
            data.putValue("shortPlayNext", spUtil.shortPlayNext)
            data.putValue("shortLifeSpeed", spUtil.shortLifeSpeed)
            data.putValue("shortRightSpeed", spUtil.shortRightSpeed)
            data.putValue("shortTitleStrategy", spUtil.shortTitleStrategy)
            data.putValue("shortTitleDelimiter", spUtil.shortTitleDelimiter!!)
            data.putValue("shortTitleRegex", spUtil.shortTitleRegex!!)
            data.putValue("shortTitleMaxLines", spUtil.shortTitleMaxLines)
            data.putValue("audioFadeEnabled", spUtil.audioFadeEnabled)
            data.putValue("audioFadeInDuration", spUtil.audioFadeInDuration)
            data.putValue("audioFadeOutDuration", spUtil.audioFadeOutDuration)
            data.putValue("coverRatio", spUtil.coverRatio!!)
            data.putValue("mediaShuffleJson", spUtil.mediaShuffleJson!!)
            data.putValue("labSurfaceRgba", spUtil.labSurfaceRgba)
            data.putValue("labSurfaceZOrder", spUtil.labSurfaceZOrder)
            data.putValue("labMpvSpecialRender", spUtil.labMpvSpecialRender)
            data.putValue("labLongVideoPortrait", spUtil.labLongVideoPortrait)
            data.putValue("longVideoPip", spUtil.longVideoPip)
            // base settings
            data.putValue("debugMode", spUtil.debugMode)
            data.putValue("showDm", spUtil.showDm)
            data.putValue("dmBold", spUtil.dmBold)
            data.putValue("dmConf", spUtil.dmConf!!)
            data.putValue("seSsData", spUtil.seSsData!!)
            data.putValue("lastFolder", spUtil.lastFolder!!)
            data.putValue("useFolders", spUtil.useFolders!!)
            data.putValue("passStroke", spUtil.passStroke)
            data.putValue("downFolder", spUtil.downFolder!!)
            data.putValue("customColor", spUtil.customColor!!)
            data.putValue("customGradient", spUtil.customGradient!!)
            data.putValue("customColorScheme", spUtil.customColorScheme!!)
            data.putValue("paletteOptions", spUtil.paletteOptions!!)
            data.putValue("downPalette", spUtil.downPalette)
            data.putValue("showDmFps", spUtil.showDmFps)
            data.putValue("iconDefault", spUtil.iconDefault)

            // 保存 playInfoList
            val basePath = applicationContext.getExternalFilesDir("info")
            basePath?.list()?.let { list ->
                val playInfoList = JSONObject()
                for (path in list) {
                    File(basePath, path).let { file ->
                        if (file.exists()) {
                            playInfoList.putValue(file.name, file.readText())
                        }
                    }
                }
                data.putValue("playInfoList", playInfoList)
            }

            val filename = "auto_backup_v${versionCode}.json"
            val backupFile = FileOperator.buildDownFile("DyLike", filename)
            FileOperator.writeText(backupFile, data.toJsonStr())

            spUtil.hasAutoBackup = true
            spUtil.lastVersionCode = CodeUtil.versionCode(applicationContext).toInt()

            Log.d("PlayerApp", "自动备份完成: $backupFile")
        } catch (e: Exception) {
            Log.d("PlayerApp", "自动备份异常: ${e.message}", e)
        }
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
