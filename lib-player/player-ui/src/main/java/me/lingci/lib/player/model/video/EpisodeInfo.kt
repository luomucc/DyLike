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
data class EpisodeInfo(
    var id: String = "",
    var animeId: String = "",
    var typeName: String = "",
    var sorting: Int = 0,
    var episodeName: String = "",
    var url: String = "",
    var subUrl: String = "",
    var del: Int = 0,
    var isSelect: Boolean = false
) : Parcelable