package me.lingci.lib.dm.view.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import java.io.File
import kotlinx.serialization.Serializable

/**
 *   @author : happyc
 *   time    : 2025/02/25
 *   desc    :
 *   version : 1.0
 */
@OptIn(InternalSerializationApi::class)
@Parcelize
@Serializable
data class DmTrack(
    var title: String = "",
    var path: String = "",
    // 单位 ms
    var offset: Long = 0,
    // view操作
    var selected: Boolean = false,
    var checked: Boolean = false,
    var showAction: Boolean = false,
    // 分割相关
    var cutStart: Int = 0,
    var cutEnd: Int = 0,
    var cutDuration: Int = 0,
    var cutMode: Int = 0,
    var mineType: String = "",
    var password: String = "",
) : Parcelable {

    constructor(file: File) : this(
        title = file.name,
        path = file.path
    )

    constructor(path: String) : this(
        File(path)
    )

}

data class DmTrackConf(
    var trackMode: DmTrackMode = DmTrackMode.SINGLE_SWITCH,
    var dmTrack: DmTrack = DmTrack()
)

enum class DmTrackMode {

    SINGLE_SWITCH,
    MULTI_MERGE

}