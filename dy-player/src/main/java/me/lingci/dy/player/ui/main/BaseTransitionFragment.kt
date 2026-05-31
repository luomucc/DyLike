package me.lingci.dy.player.ui.main

import android.os.Bundle
import com.google.android.material.transition.MaterialFadeThrough
import me.lingci.lib.base.ui.BaseFragment

/**
 *   @author : happyc
 *   time    : 2025/03/23
 *   desc    :
 *   version : 1.0
 */
open class BaseTransitionFragment: BaseFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialFadeThrough()
        enterTransition = MaterialFadeThrough()
    }

    override fun resetView() {

    }

    protected fun isOrientationPortraitOfSysMetrics(): Boolean =
        getSysHeightPixels() >= getSysWidthPixels()

    protected fun getSysHeightPixels(): Int =
        resources.displayMetrics.heightPixels

    protected fun getSysWidthPixels(): Int =
        resources.displayMetrics.widthPixels

}