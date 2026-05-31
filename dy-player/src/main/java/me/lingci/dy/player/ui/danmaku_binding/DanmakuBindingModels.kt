package me.lingci.dy.player.ui.danmaku_binding

import me.lingci.dy.player.entity.VideoData
import me.lingci.dy.player.ui.long_video.PlayInfo
import me.lingci.lib.base.storage.entity.FileEntity
import me.lingci.lib.dm.view.entity.DmTrack
import java.io.File

data class DanmakuBindingMediaItem(
    val videoData: VideoData,
    var originalInfo: PlayInfo? = null,
    var originalTracks: MutableList<DmTrack> = mutableListOf(),
    var originalLastTrackPath: String = "",
    var workingTracks: MutableList<DmTrack> = mutableListOf(),
    var lastTrackPath: String = "",
    var selected: Boolean = false,
    var matchHint: String = ""
) {

    fun boundCount(): Int {
        return workingTracks.size
    }

    fun primaryTrack(): DmTrack? {
        return workingTracks.firstOrNull { it.path.samePath(lastTrackPath) }
            ?: workingTracks.firstOrNull { it.selected }
            ?: workingTracks.firstOrNull()
    }

    fun hasTrack(path: String): Boolean {
        return workingTracks.any { it.path.samePath(path) }
    }

    fun isPrimary(path: String): Boolean {
        return primaryTrack()?.path?.samePath(path) == true
    }

    fun hasChanges(): Boolean {
        return originalLastTrackPath.samePath(lastTrackPath).not() ||
                originalTracks.bindingSignature() != workingTracks.bindingSignature()
    }
}

data class DanmakuBindingFileItem(
    val fileEntity: FileEntity,
    val score: Int = 0,
    val ownerTitle: String = "",
    val boundToSelected: Boolean = false,
    val primaryForSelected: Boolean = false
)

fun DmTrack.bindingCopy(): DmTrack {
    return copy(checked = false, showAction = false)
}

fun DmTrack.displayNameWithoutExtension(): String {
    return title.ifBlank { path.substringAfterLast('/') }
}

fun FileEntity.displayNameWithoutExtension(): String {
    return name.ifBlank { title }
}

fun String.removeExtensionForDisplay(): String {
    val value = trim()
    val index = value.lastIndexOf('.')
    return if (index > 0) value.take(index) else value
}

fun DmTrack.isUsableBindingTrack(): Boolean {
    if (path.isBlank()) {
        return false
    }
    return path.startsWith("http") || File(path).exists()
}

fun List<DmTrack>.bindingSignature(): List<String> {
    return map { track ->
        listOf(
            track.path.normalizeBindingPath(),
            track.title.trim(),
            track.offset.toString(),
            track.mineType.trim(),
            track.password.trim(),
            track.selected.toString()
        ).joinToString("|")
    }.sorted()
}

fun String.samePath(other: String): Boolean {
    return normalizeBindingPath() == other.normalizeBindingPath()
}

fun String.normalizeBindingPath(): String {
    return trim().replace('\\', '/').lowercase()
}
