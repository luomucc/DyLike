package me.lingci.dy.player.ui.media

import android.app.Activity
import me.lingci.dy.player.entity.CoverType
import me.lingci.dy.player.entity.MediaData
import me.lingci.dy.player.entity.MediaLibType
import me.lingci.dy.player.entity.VideoData
import me.lingci.dy.player.util.LibraryCompat
import me.lingci.dy.player.util.SpUtil
import me.lingci.lib.base.storage.entity.StorageType
import me.lingci.lib.base.util.Log

/**
 *   @author : happyc
 *   time    : 2025/05/02
 *   desc    : 媒体库辅助类
 *   version : 1.0
 */
object MediaHelper {

    fun createMediaFromString(linkString: String): MediaData {
        val mediaData = MediaData().apply {
            type = MediaLibType.ONLINE
            storageType = StorageType.ONLINE_LINK
        }
        var start = ""
        val videoList = mutableListOf<VideoData>()
        val headers = mutableMapOf<String, String>()
        linkString.replace("\r\n", "\n").split("\n").forEach { line ->
            if (line.startsWith("#")) {
                start = line.substringAfterLast("#")
                return@forEach
            }
            if (line.trim().isBlank()) {
                return@forEach
            }
            when (start.trim().uppercase()) {
                "EP" -> {
                    Log.d(this@MediaHelper, start, line)
                    val video = VideoData().apply {
                        type = StorageType.ONLINE_LINK
                    }
                    if (line.contains(";")) {
                        video.name = line.substringBefore(";")
                        video.videoUrl = line.substringAfter(";")
                        videoList.add(video)
                    } else {
                        if (line.trim().startsWith("http")) {
                            video.videoUrl = line.trim()
                            videoList.add(video)
                        }
                    }
                }

                "NAME" -> {
                    mediaData.title = line.trim()
                }

                "IMAGE" -> {
                    mediaData.showFile = line.trim()
                    mediaData.coverType = CoverType.CUSTOM
                }

                "SOURCE" -> {
                    mediaData.playType = line.trim()
                }

                "HEAD" -> {
                    line.count { it == ':' }.let { count ->
                        if (line.startsWith(":") && count == 2) {
                            headers[line.substringBeforeLast(":")] =
                                line.substringAfterLast(":")
                        } else {
                            headers[line.substringBefore(":")] = line.substringAfter(":")
                        }
                    }
                }
            }
        }
        Log.d(this@MediaHelper, mediaData, videoList)
        if (videoList.isNotEmpty()) {
            videoList.forEach {
                it.headers = headers
            }
        }
        mediaData.items = videoList
        mediaData.id = LibraryCompat.mediaId(mediaData)
        return mediaData
    }

    fun addMedia(spUtil: SpUtil, mediaData: MediaData): Boolean {
        val sources = LibraryCompat.loadSources(spUtil)
        if (mediaData.id.isBlank()) {
            mediaData.id = LibraryCompat.mediaId(mediaData, sources)
        }
        val list = LibraryCompat.loadMedia(spUtil)
        val index = list.indexOfFirst { media ->
            LibraryCompat.sameMedia(media, mediaData, sources)
        }
        if (index == -1) {
            list.add(mediaData)
            LibraryCompat.saveMedia(spUtil, list)
            return true
        }
        return false
    }

    fun updateMedia(spUtil: SpUtil, mediaData: MediaData) {
        val sources = LibraryCompat.loadSources(spUtil)
        if (mediaData.id.isBlank()) {
            mediaData.id = LibraryCompat.mediaId(mediaData, sources)
        }
        val list = LibraryCompat.loadMedia(spUtil)
        val index = list.indexOfFirst { media ->
            LibraryCompat.sameMedia(media, mediaData, sources)
        }
        if (index != -1) {
            list[index] = mediaData
            LibraryCompat.saveMedia(spUtil, list)
        }
    }

    fun startDetail(activity: Activity, mediaData: MediaData, showDialog: Boolean = false) {

    }

}
