package me.lingci.dy.player.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 *   @author : happyc
 *   time    : 2024/09/23
 *   desc    :
 *   version : 1.0
 */
@OptIn(InternalSerializationApi::class)
@Serializable
@Parcelize
data class VersionData(

    var info: String = "",
    var type: Int = 0,
    var versionCode: Long = 0L,
    var downUrl: String = "",
    var remark: String = ""

) : Parcelable {
}