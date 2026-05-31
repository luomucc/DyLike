package me.lingci.lib.dm.view.parser

import master.flame.danmaku.danmaku.model.IDanmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import me.lingci.lib.base.util.WeightedRandomSelector
import me.lingci.lib.dm.view.entity.DmLoadOptions

/**
 *   @author : happyc
 *   time    : 2025/09/02
 *   desc    : https://juejin.cn/post/6844903764910931975 https://github.com/bilibili/DanmakuFlameMaster/issues/323 setOnDanmakuClickListener
 *   version : 1.0
 */
open class MultiDanmakuParser(
    val options: DmLoadOptions = DmLoadOptions()
) : BaseDanmakuParser(){

    private val randomSelector = WeightedRandomSelector(
        listOf(
            Pair(true, options.gradientRatio),
            Pair(false, Integer.max(0, 100 - options.gradientRatio))
        )
    )

    override fun parse(): IDanmakus? {


        return null
    }

}