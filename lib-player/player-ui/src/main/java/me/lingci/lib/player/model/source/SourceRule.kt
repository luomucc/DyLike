package me.lingci.lib.player.model.source

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * @author : happyc
 * time    : 2024/08/22
 * desc    :
 * version : 1.0
 */
@OptIn(InternalSerializationApi::class)
@Parcelize
@Serializable
data class SourceRule(
    var id: String = "",
    var enable: Boolean = true,
    var name: String = "",
    var baseurl: String = "",
    var subUrl: String = "",
    var horizontal: Boolean = false,
    var publishUrl: String = "",
    var baseurlRule: String = "",
    var searchUrl: String = "",
    var weekUrl: String = "",
    var weekIndexRule: String = "",
    var videoListRule: String = "",
    var videoNameRule: String = "",
    var videoImageRule: String = "",
    var videoImageAttrRule: String = "",
    var videoLabelRule: String = "",
    var videoDescriptionRule: String = "",
    var videoUrlRule: String = "",
    var videoUrlAttrRule: String = "",
    var videoInfoRule: String = "",
    var videoInfoLabelListRule: String = "",
    var videoInfoDescriptionRule: String = "",
    var episodeListRule: String = "",
    var episodeNameRule: String = "",
    var episodeUrlRule: String = "",
    var episodeUrlAttrRule: String = "",
    var playUrlRule: String = "",
) : Parcelable {

    fun buildSearchUrl(wd: String): String {
        return String.format(searchUrl, baseurl, wd)
    }

    fun buildWeekUrl(index: String): String {
        return String.format(weekUrl, baseurl, index)
    }

    fun weekEnable(): Boolean {
        return weekUrl.isNotBlank()
    }

}