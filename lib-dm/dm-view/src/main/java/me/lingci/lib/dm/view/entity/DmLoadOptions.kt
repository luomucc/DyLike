package me.lingci.lib.dm.view.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 *   @author : happyc
 *   time    : 2025/04/04
 *   desc    : 弹目加载配置项
 *   version : 1.0
 */
@Parcelize
data class DmLoadOptions(

    // 白色转为渐变
    var whiteToGradient: Boolean = false,
    // 渐变转换比例
    var gradientRatio: Int = 40,
    // 渐变模式加载字体颜色，否则转为白色
    var gradientWithTextColor: Boolean = false,
    // 默认渐变色
    var colors: List<Int>? = null,
    // time debug
    var timeDebug: Boolean = false,
    // 合并
    var mergeContent: Boolean = false,
    // 合并显示x
    var mergeShow: Int = 9,
    // 转换为顶部
    var mergeToTop: Int = 9,
    var debug: Boolean = false,

) : Serializable, Parcelable {
}