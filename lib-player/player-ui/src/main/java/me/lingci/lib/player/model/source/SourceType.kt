package me.lingci.lib.player.model.source

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @author : happyc
 * time    : 2022/05/17
 * desc    :
 * version : 1.0
 */
@OptIn(InternalSerializationApi::class)
@Parcelize
@Serializable
data class SourceType(
    var type: String = "",
    var url: String = "",
    var sid: String = "",
    var select: Boolean = false,
    var sourceRule: SourceRule? = null,
) : Parcelable {

}