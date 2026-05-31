package me.lingci.lib.player.model.video

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import me.lingci.lib.player.model.source.SourceType

/**
 * @author : happyc
 * time    : 2022/05/06
 * desc    : 扩展解析
 * version : 1.0
 */
@OptIn(InternalSerializationApi::class)
@Parcelize
@Serializable
data class VideoInfo(
    var id: String =  "",
    var titleName: String =  "",
    var collection: String =  "",
    var description: String =  "",
    var img: String =  "",
    var tag: String =  "",
    var area: String =  "",
    var years: String =  "",
    var updateTime: String =  "",
    var remark: String =  "",
    var del: Int =  0,
    var type: String =  "",
    var url: String =  "",
    var sid: String =  "",
    var collectionList: MutableList<VideoInfo> =  mutableListOf(),
    var episodeList: MutableList<EpisodeType> =  mutableListOf(),
    var types: MutableList<SourceType> =  mutableListOf(),
    var weekIndex: Int =  0,
) : Parcelable {

}