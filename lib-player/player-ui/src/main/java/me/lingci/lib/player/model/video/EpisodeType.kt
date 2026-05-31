package me.lingci.lib.player.model.video

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @author : happyc
 * time    : 2022/05/06
 * desc    :
 * version : 1.0
 */
@OptIn(InternalSerializationApi::class)
@Parcelize
@Serializable
data class EpisodeType(
    var typeName: String? = null,
    var episodeList: ArrayList<EpisodeInfo>? = null,
    var isSelect: Boolean = false
) : Parcelable {

}