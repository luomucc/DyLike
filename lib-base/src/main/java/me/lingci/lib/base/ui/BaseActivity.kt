package me.lingci.lib.base.ui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.lingci.lib.base.dailog.LoadingDialog
import me.lingci.lib.base.enums.WindowSizeEnum

abstract class BaseActivity : BaseDisplayActivity() {

    companion object {
        const val TAG = "lcDev"
    }

    private lateinit var loadingDialog: LoadingDialog

    protected val activityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                resultSuccess(it)
            }
            if (it.resultCode != RESULT_OK) {
                resultFailed(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadingDialog = LoadingDialog(this)
    }

    protected fun showLoading() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (!loadingDialog.isShowing) {
                loadingDialog.show()
            }
        }
    }

    protected fun hideLoading() {
        lifecycleScope.launch(Dispatchers.Main) {
            safeDismissDialog()
        }
    }

    private fun safeDismissDialog() {
        if (!this.isDestroyed && !this.isFinishing) {
            if (loadingDialog.isShowing) {
                loadingDialog.dismiss()
            }
        }
    }

    open fun resultSuccess(result: ActivityResult) {}
    open fun resultFailed(result: ActivityResult) {}

    protected fun setSystemWindowsFalse() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    protected fun setSystemWindowsTrue() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
    }

    /**
     * 切换全屏，屏幕常量
     * @param fullscreen 全屏
     */
    @SuppressLint("SourceLockedOrientationActivity")
    protected fun toggleFullscreen(fullscreen: Boolean) {
        if (fullscreen) {
            // 设置横屏
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            // 隐藏状态栏
            hideSysBar()
            // 常亮
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            // 设置竖屏
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            // 显示状态栏
            showSysBar()
            // 清除常亮
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    protected fun hideSysBar() {
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val decorView = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            decorView.windowInsetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars())
                controller.hide(WindowInsets.Type.navigationBars())
            }
        } else {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = decorView.systemUiVisibility or
                    (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    protected fun showSysBar() {
        @Suppress("DEPRECATION")
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val decorView = window.decorView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            decorView.windowInsetsController?.let { controller ->
                controller.show(WindowInsets.Type.statusBars())
                controller.show(WindowInsets.Type.navigationBars())
            }
        } else {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    /**
     * 设置状态栏文字深色，同时保留之前的flag
     */
    @Suppress("DEPRECATION")
    protected fun setSystemUiLightStatus() {
        val decorView = window.decorView
        var originFlag = decorView.systemUiVisibility
        originFlag = originFlag or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        decorView.systemUiVisibility = originFlag
    }

    /**
     * 清除状态栏文字深色，同时保留之前的flag
     */
    @Suppress("DEPRECATION")
    protected fun clearSystemUiLightStatus() {
        val decorView = window.decorView
        var originFlag = decorView.systemUiVisibility
        // 使用异或清除SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        originFlag = originFlag xor View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        decorView.systemUiVisibility = originFlag
    }

    protected fun setOrientation() {
        requestedOrientation = if (compactScreen())
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
    }

    protected fun computeWindowSizeClasses(): Pair<WindowSizeEnum, WindowSizeEnum> {
        val metrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)

        val widthDp = metrics.bounds.width() / resources.displayMetrics.density
        val widthWindowSize = when {
            widthDp < 600f -> WindowSizeEnum.COMPACT
            widthDp < 840f -> WindowSizeEnum.MEDIUM
            else -> WindowSizeEnum.EXPANDED
        }

        val heightDp = metrics.bounds.height() / resources.displayMetrics.density
        val heightWindowSize = when {
            heightDp < 480f -> WindowSizeEnum.COMPACT
            heightDp < 900f -> WindowSizeEnum.MEDIUM
            else -> WindowSizeEnum.EXPANDED
        }

        return Pair(widthWindowSize, heightWindowSize)
    }

    /** Determines whether the device has a compact screen. **/
    protected fun compactScreen(): Boolean {
        val screenMetrics = WindowMetricsCalculator
            .getOrCreate()
            .computeMaximumWindowMetrics(this)
        val shortSide = Integer.min(
            screenMetrics.bounds.width(),
            screenMetrics.bounds.height()
        )
        return shortSide / resources.displayMetrics.density < 600
    }

    /**
     * 判断是否是平板设备
     */
    protected fun isTablet(): Boolean {
        return (resources.configuration.screenLayout
                and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    /**
     * 判断屏幕方向
     */
    open fun isScreenPortrait(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    protected fun isOrientationPortraitOfSysMetrics(): Boolean =
        getSysHeightPixels() >= getSysWidthPixels()

    protected fun getSysHeightPixels(): Int =
        resources.displayMetrics.heightPixels

    protected fun getSysWidthPixels(): Int =
        resources.displayMetrics.widthPixels

}