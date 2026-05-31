package me.lingci.lib.player.widget.linstener

import me.lingci.lib.base.view.EffectType

/**
 *   @author : happyc
 *   time    : 2025/03/30
 *   desc    :
 *   version : 1.0
 */
interface VideoScaleListener {

    /**
     * 缩放比例
     */
    fun onScale(scale: Float)

    /**
     * 上下滑动
     */
    fun onScroll(delta: Float, max: Float, type: Int = 1)

    fun onEffectChange(type: EffectType?)

}