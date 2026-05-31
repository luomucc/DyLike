package me.lingci.lib.dm.view.common

import me.lingci.lib.base.util.FileOperator

/**
 *   @author : happyc
 *   time    : 2025/04/04
 *   desc    :
 *   version : 1.0
 */
@Suppress("unused", "SpellCheckingInspection")
object DmInitializer {

    const val DANMAKU_SOURCE = "ZhXs"

    const val DANMAKU_TAG_I = "i"
    const val DANMAKU_TAG_D = "d"
    const val DANMAKU_TAG_CHAT_SERVER = "chatserver"
    const val DANMAKU_TAG_CHATID = "chatid"
    const val DANMAKU_TAG_MISSION = "mission"
    const val DANMAKU_TAG_MAXLIMIT = "maxlimit"
    const val DANMAKU_TAG_MAXCOUNT = "maxcount"
    const val DANMAKU_TAG_SOURCE = "source"

    const val DANMAKU_ATTR_P = "p"
    const val DANMAKU_ATTR_S = "s"

    const val FILTER_SEPARATOR = ",,"

    const val GRADIENT_SEPARATOR = "~"
    const val GRADIENT_TYPE_STROKE = 0
    const val GRADIENT_TYPE_TEXT = 1

    var SSES_DATA: String = ""
    var DOWN_PATH: String = FileOperator.buildDownFile("弹目").path

    fun updateDownPath(path: String) {
        DOWN_PATH = path
    }

}