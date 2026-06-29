package me.lingci.dy.player.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.lingci.dy.player.R
import me.lingci.dy.player.databinding.ActivityMainBinding
import me.lingci.dy.player.entity.VersionData
import me.lingci.dy.player.ui.tool.BackupSettingsFragment
import me.lingci.dy.player.util.AppUtil
import me.lingci.dy.player.util.SpUtil
import me.lingci.lib.base.okhttp.httpGet
import me.lingci.lib.base.ui.BaseDisplayActivity
import me.lingci.lib.base.util.CodeUtil
import me.lingci.lib.base.util.JsonUtil
import me.lingci.lib.base.util.Log
import me.lingci.lib.base.util.ToastUtil
import me.lingci.lib.base.util.createNew
import java.io.File
import java.lang.reflect.Method

/**
 * 主页
 */
class MainActivity : BaseDisplayActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val spUtil by lazy { SpUtil(this) }
    private lateinit var binding: ActivityMainBinding
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        // 使更多菜单带图标
        if (menu.javaClass.simpleName.equals("MenuBuilder", ignoreCase = true)) {
            try {
                val method: Method = menu.javaClass.getDeclaredMethod(
                    "setOptionalIconsVisible",
                    java.lang.Boolean.TYPE
                )
                method.isAccessible = true
                method.invoke(menu, true)
            } catch (_: Exception) {

            }
        }
        return super.onMenuOpened(featureId, menu)
    }

    private fun init() {
        initView()
        initBackPressed()
        initResult()
        initUpdate()
        checkAndRestoreSettings()
    }

    /**
     * 检测是否有旧版本备份，如果有则提示导入
     */
    private fun checkAndRestoreSettings() {
        try {
            val spUtil = spUtil
            val currentVersionCode = CodeUtil.versionCode(this).toInt()
            val lastVersionCode = spUtil.lastVersionCode

            // 如果版本号发生变化，说明是升级后首次启动
            if (lastVersionCode > 0 && currentVersionCode != lastVersionCode) {
                // 查找旧版本的自动备份文件
                val backupDir = File(android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS), "DyLike")
                if (backupDir.exists()) {
                    val backupFiles = backupDir.listFiles { file ->
                        file.name.startsWith("auto_backup_v") && file.name.endsWith(".json")
                    }

                    if (!backupFiles.isNullOrEmpty()) {
                        // 找到最新的备份文件
                        val latestBackup = backupFiles.maxByOrNull { it.lastModified() }
                        latestBackup?.let { file ->
                            val backupVersion = file.name.replace("auto_backup_v", "").replace(".json", "")
                            showRestoreDialog(file.absolutePath, backupVersion)
                        }
                    }
                }
            }

            // 更新版本号
            if (lastVersionCode != currentVersionCode) {
                spUtil.lastVersionCode = currentVersionCode
            }
        } catch (e: Exception) {
            Log.d("MainActivity", "检测备份失败: ${e.message}", e)
        }
    }

    private fun showRestoreDialog(backupPath: String, backupVersion: String) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("检测到旧版本备份")
            .setMessage("发现版本 $backupVersion 的自动备份，是否导入设置？")
            .setPositiveButton("导入") { _, _ ->
                restoreFromFile(backupPath)
            }
            .setNegativeButton("跳过") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
        dialog.show()
    }

    private fun restoreFromFile(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                ToastUtil.showToast(this, "备份文件不存在")
                return
            }
            val dataJson = me.lingci.lib.base.util.FileOperator.readText(filePath)
            val data = me.lingci.lib.base.json.JSONObject(dataJson)
            val spUtil = spUtil

            // base
            if (data.hasKey("debugMode")) { spUtil.debugMode = data.getBoolean("debugMode") }
            if (data.hasKey("showDm")) { spUtil.showDm = data.getBoolean("showDm") }
            if (data.hasKey("dmBold")) { spUtil.dmBold = data.getBoolean("dmBold") }
            if (data.hasKey("dmConf")) { spUtil.dmConf = data.getString("dmConf") }
            if (data.hasKey("seSsData")) { spUtil.seSsData = data.getString("seSsData") }
            if (data.hasKey("lastFolder")) { spUtil.lastFolder = data.getString("lastFolder") }
            if (data.hasKey("useFolders")) { spUtil.useFolders = data.getString("useFolders") }
            if (data.hasKey("passStroke")) { spUtil.passStroke = data.getBoolean("passStroke") }
            if (data.hasKey("downFolder")) { spUtil.downFolder = data.getString("downFolder") }
            if (data.hasKey("customColor")) { spUtil.customColor = data.getString("customColor") }
            if (data.hasKey("customGradient")) { spUtil.customGradient = data.getString("customGradient") }
            if (data.hasKey("customColorScheme")) { spUtil.customColorScheme = data.getString("customColorScheme") }
            if (data.hasKey("paletteOptions")) { spUtil.paletteOptions = data.getString("paletteOptions") }
            if (data.hasKey("downPalette")) { spUtil.downPalette = data.getBoolean("downPalette") }
            if (data.hasKey("showDmFps")) { spUtil.showDmFps = data.getBoolean("showDmFps") }
            if (data.hasKey("iconDefault")) { spUtil.iconDefault = data.getBoolean("iconDefault") }
            if (data.hasKey("labSurfaceRgba")) { spUtil.labSurfaceRgba = data.getBoolean("labSurfaceRgba") }
            if (data.hasKey("labSurfaceZOrder")) { spUtil.labSurfaceZOrder = data.getBoolean("labSurfaceZOrder") }
            if (data.hasKey("labMpvSpecialRender")) { spUtil.labMpvSpecialRender = data.getBoolean("labMpvSpecialRender") }

            // app
            if (data.hasKey("isFirst")) { spUtil.isFirst = data.getBoolean("isFirst") }
            if (data.hasKey("dataSchemaVersion")) { spUtil.dataSchemaVersion = data.getInt("dataSchemaVersion") }
            if (data.hasKey("sourceJson")) { spUtil.sourceJson = data.getString("sourceJson") }
            if (data.hasKey("mediaJson")) { spUtil.mediaJson = data.getString("mediaJson") }
            if (data.hasKey("historyJson")) { spUtil.historyJson = data.getString("historyJson") }
            if (data.hasKey("likeJson")) { spUtil.likeJson = data.getString("likeJson") }
            if (data.hasKey("playlistJson")) { spUtil.playlistJson = data.getString("playlistJson") }
            if (data.hasKey("dayStr")) { spUtil.dayStr = data.getString("dayStr") }
            if (data.hasKey("videoDetailMode")) { spUtil.videoDetailMode = data.getBoolean("videoDetailMode") }
            if (data.hasKey("videoPlayerCore")) { spUtil.videoPlayerCore = data.getInt("videoPlayerCore") }
            if (data.hasKey("shortVideoPlayerCore")) { spUtil.shortVideoPlayerCore = data.getInt("shortVideoPlayerCore") }
            if (data.hasKey("longVideoMode")) { spUtil.longVideoMode = data.getBoolean("longVideoMode") }
            if (data.hasKey("shortRandom")) { spUtil.shortRandom = data.getBoolean("shortRandom") }
            if (data.hasKey("dmGradientMode")) { spUtil.dmGradientMode = data.getBoolean("dmGradientMode") }
            if (data.hasKey("dmGradientRatio")) { spUtil.dmGradientRatio = data.getInt("dmGradientRatio") }
            if (data.hasKey("dmGradientWithTextColor")) { spUtil.dmGradientWithTextColor = data.getBoolean("dmGradientWithTextColor") }
            if (data.hasKey("dmStrokeMultipleMode")) { spUtil.dmStrokeMultipleMode = data.getBoolean("dmStrokeMultipleMode") }
            if (data.hasKey("dmStrokeMultiple")) { spUtil.dmStrokeMultiple = data.getDouble("dmStrokeMultiple").toFloat() }
            if (data.hasKey("dmFontMode")) { spUtil.dmFontMode = data.getBoolean("dmFontMode") }
            if (data.hasKey("dmCurrentFont")) { spUtil.dmCurrentFont = data.getString("dmCurrentFont") }
            if (data.hasKey("firstScanMovie")) { spUtil.firstScanMovie = data.getBoolean("firstScanMovie") }
            if (data.hasKey("surfaceRender")) { spUtil.surfaceRender = data.getBoolean("surfaceRender") }
            if (data.hasKey("dmMergeMode")) { spUtil.dmMergeMode = data.getBoolean("dmMergeMode") }
            if (data.hasKey("dmShowTime")) { spUtil.dmShowTime = data.getBoolean("dmShowTime") }
            if (data.hasKey("dmMergeTop")) { spUtil.dmMergeTop = data.getInt("dmMergeTop") }
            if (data.hasKey("dmMergeShow")) { spUtil.dmMergeShow = data.getInt("dmMergeShow") }
            if (data.hasKey("browserUsedAll")) { spUtil.browserUsedAll = data.getBoolean("browserUsedAll") }
            if (data.hasKey("browserSort")) { spUtil.browserSort = data.getInt("browserSort") }
            if (data.hasKey("browserShowHide")) { spUtil.browserShowHide = data.getBoolean("browserShowHide") }
            if (data.hasKey("newHome")) { spUtil.newHome = data.getBoolean("newHome") }
            if (data.hasKey("sortRender")) { spUtil.sortRender = data.getBoolean("sortRender") }
            if (data.hasKey("useOkhttp")) { spUtil.useOkhttp = data.getBoolean("useOkhttp") }
            if (data.hasKey("autoNext")) { spUtil.autoNext = data.getBoolean("autoNext") }
            if (data.hasKey("loopList")) { spUtil.loopList = data.getBoolean("loopList") }
            if (data.hasKey("showShortTitle")) { spUtil.showShortTitle = data.getBoolean("showShortTitle") }
            if (data.hasKey("showShortLike")) { spUtil.showShortLike = data.getBoolean("showShortLike") }
            if (data.hasKey("showShortComment")) { spUtil.showShortComment = data.getBoolean("showShortComment") }
            if (data.hasKey("showShortPager")) { spUtil.showShortPager = data.getBoolean("showShortPager") }
            if (data.hasKey("shortPlayNext")) { spUtil.shortPlayNext = data.getBoolean("shortPlayNext") }
            if (data.hasKey("shortLifeSpeed")) { spUtil.shortLifeSpeed = data.getFloat("shortLifeSpeed") }
            if (data.hasKey("shortRightSpeed")) { spUtil.shortRightSpeed = data.getFloat("shortRightSpeed") }
            if (data.hasKey("shortTitleStrategy")) { spUtil.shortTitleStrategy = data.getInt("shortTitleStrategy") }
            if (data.hasKey("shortTitleDelimiter")) { spUtil.shortTitleDelimiter = data.getString("shortTitleDelimiter") }
            if (data.hasKey("shortTitleRegex")) { spUtil.shortTitleRegex = data.getString("shortTitleRegex") }
            if (data.hasKey("shortTitleMaxLines")) { spUtil.shortTitleMaxLines = data.getInt("shortTitleMaxLines") }
            if (data.hasKey("audioFadeEnabled")) { spUtil.audioFadeEnabled = data.getBoolean("audioFadeEnabled") }
            if (data.hasKey("audioFadeInDuration")) { spUtil.audioFadeInDuration = data.getInt("audioFadeInDuration") }
            if (data.hasKey("audioFadeOutDuration")) { spUtil.audioFadeOutDuration = data.getInt("audioFadeOutDuration") }
            if (data.hasKey("coverRatio")) { spUtil.coverRatio = data.getString("coverRatio") }
            if (data.hasKey("mediaShuffleJson")) { spUtil.mediaShuffleJson = data.getString("mediaShuffleJson") }
            if (data.hasKey("labSurfaceRgba")) { spUtil.labSurfaceRgba = data.getBoolean("labSurfaceRgba") }
            if (data.hasKey("labSurfaceZOrder")) { spUtil.labSurfaceZOrder = data.getBoolean("labSurfaceZOrder") }
            if (data.hasKey("labMpvSpecialRender")) { spUtil.labMpvSpecialRender = data.getBoolean("labMpvSpecialRender") }
            if (data.hasKey("labLongVideoPortrait")) { spUtil.labLongVideoPortrait = data.getBoolean("labLongVideoPortrait") }
            if (data.hasKey("longVideoPip")) { spUtil.longVideoPip = data.getBoolean("longVideoPip") }

            // playInfoList
            if (data.hasKey("playInfoList")) {
                val playInfo = data.getJSONObject("playInfoList")
                getExternalFilesDir("info")?.let { infoFile ->
                    for (name in playInfo.keys()) {
                        File(infoFile, name).apply {
                            if (exists().not()) { createNew() }
                            writeText(playInfo.getString(name))
                        }
                    }
                }
            }

            me.lingci.dy.player.util.LibraryCompat.migrateIfNeeded(spUtil)
            ToastUtil.showToast(this, "设置导入成功，请重启应用")
        } catch (e: Exception) {
            ToastUtil.showToast(this, "导入失败: ${e.message}")
        }
    }

    private fun initView() {
        setSupportActionBar(binding.toolbar)
        initNav()
    }

    private fun initNav() {
        val navView: BottomNavigationView = binding.navView
        val navController = binding.navHostFragmentActivityMain
            .getFragment<NavHostFragment>()
            .navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_media, R.id.navigation_source, R.id.navigation_tool
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun initBackPressed() {
        onBackPressedDispatcher.addCallback(this) {
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // 确保意图能被解析
                    if (resolveActivity(packageManager) == null) {
                        // 如果没有找到能处理的组件，做降级处理
                        finish()
                        return@apply
                    }
                }
                startActivity(homeIntent)
            } catch (e: Exception) {
                Log.d(this, "go home failed ${e.message}")
                finish()
            }
        }
    }

    private fun initResult() {
        resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == RESULT_OK) {
                Log.d(TAG, "initResult: ${it.data}")
                return@registerForActivityResult
            } else {
                Log.d(TAG, "initResult: ${it.resultCode}")
            }
        }
    }

    private fun initUpdate() {
        val today = AppUtil.today()
        if (spUtil.dayStr!!.startsWith(today)) {
            return
        }
        lifecycleScope.launch {
            httpGet("https://gitee.com/happycao/static-file/raw/api/app/api/dy_like_version.json")
                .execute()
                .onSuccess { response ->
                    val versionData = JsonUtil.toEntityCbc<VersionData>(response)
                    withContext(Dispatchers.Main) {
                        val versionCode = CodeUtil.versionCode(this@MainActivity)
                        if (versionData.type == 0 && versionCode < versionData.versionCode && spUtil.isFirst) {
                            spUtil.isFirst = false
                            val versionDialog = VersionDialog.newInstance(versionData, null)
                            versionDialog.show(supportFragmentManager, "app-version")
                        }
                        if (versionCode < versionData.versionCode) {
                            val dayStr = spUtil.dayStr!!.split("#")
                            if (dayStr.size > 1) {
                                if (dayStr[0] == today) {
                                    return@withContext
                                }
                                if (versionData.type == 0 && dayStr[1] == "${versionData.versionCode}") {
                                    return@withContext
                                }
                            }
                            val versionDialog = VersionDialog.newInstance(versionData) {
                                resultLauncher.launch(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        versionData.downUrl.toUri()
                                    )
                                )
                            }
                            versionDialog.show(supportFragmentManager, versionDialog.tag)
                        }
                        spUtil.dayStr = "$today#${versionData.versionCode}"
                    }
                }.onFailure { e ->
                    spUtil.dayStr = "$today#"
                    Log.d(TAG, "initUpdate: ${e.message}", e)
                }
        }
    }

    private fun modeNight() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
    }

    private fun changeModeNight() {
        delegate.localNightMode = if (delegate.localNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        recreate()
    }

}
