package me.lingci.lib.dm.view.entity

/**
 *   @author : happyc
 *   time    : 2025/03/27
 *   desc    : 弹幕样式扩展
 *   version : 1.0
 */
data class DmStyleExtend(
    var gradientColors: List<Int>? = null,
    var strokeMode: Boolean = true
) {

    private var gradientColorArrayCache: IntArray? = null
    private var gradientHashCache: Int? = null

    constructor(colors: List<Int>): this (
        gradientColors = colors
    )

    fun gradientColorArray(): IntArray? {
        val colors = gradientColors ?: return null
        val cached = gradientColorArrayCache
        if (cached != null) {
            return cached
        }
        return colors.toIntArray().also {
            gradientColorArrayCache = it
        }
    }

    fun gradientHash(): Int? {
        val cached = gradientHashCache
        if (cached != null) {
            return cached
        }
        val hash = gradientColorArray()?.contentHashCode() ?: return null
        gradientHashCache = hash
        return hash
    }

}
