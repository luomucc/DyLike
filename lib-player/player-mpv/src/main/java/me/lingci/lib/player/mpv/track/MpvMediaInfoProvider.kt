package me.lingci.lib.player.mpv.track

import me.lingci.lib.player.mpv.MpvMediaPlayer
import me.lingci.lib.player.mpv.MpvUtil
import me.lingci.lib.player.mediainfo.AudioTrackInfo
import me.lingci.lib.player.mediainfo.ContainerInfo
import me.lingci.lib.player.mediainfo.MediaInfoData
import me.lingci.lib.player.mediainfo.MediaInfoProvider
import me.lingci.lib.player.mediainfo.SubtitleTrackInfo
import me.lingci.lib.player.mediainfo.VideoTrackInfo

class MpvMediaInfoProvider(
    private val mpv: MpvMediaPlayer
) : MediaInfoProvider {

    private val m get() = mpv.mpv

    override fun getMediaInfo(): MediaInfoData {
        // Combine MPV's current scalar properties with the cached track list. Some properties only
        // exist for the selected stream, so non-selected tracks intentionally carry sparse details.
        val trackManager = mpv.getTrackManager()
        val psc = mpv.psc

        val hwdec = safeGetString("hwdec-current") ?: "no"
        val isHwDecode = hwdec != "no" && hwdec.isNotBlank()

        return MediaInfoData(
            containerInfo = ContainerInfo(
                duration = mpv.duration,
                currentPosition = psc.position,
                containerFormat = safeGetString("file-format") ?: "",
                fileSize = safeGetLong("file-size"),
                playState = resolvePlayState(psc),
            ),
            videoTracks = trackManager.videoTracks.map { it.toVideoTrackInfo(psc, isHwDecode, hwdec) },
            audioTracks = trackManager.audioTracks.map { it.toAudioTrackInfo() },
            subtitleTracks = trackManager.subtitleTracks.map { it.toSubtitleTrackInfo() }
        )
    }

    override fun isAvailable(): Boolean {
        val trackManager = mpv.getTrackManager()
        return mpv.duration > 0L ||
            trackManager.videoTracks.isNotEmpty() ||
            trackManager.audioTracks.isNotEmpty() ||
            trackManager.subtitleTracks.isNotEmpty() ||
            !safeGetString("file-format").isNullOrBlank() ||
            !safeGetString("path").isNullOrBlank() ||
            !safeGetString("stream-open-filename").isNullOrBlank()
    }

    private fun resolvePlayState(psc: MpvUtil.PlaybackStateCache): String {
        return when {
            psc.cachePause -> "缓冲中"
            psc.pause -> "暂停"
            mpv.isPlaying -> "播放中"
            else -> "就绪"
        }
    }

    private fun TrackInfo.toVideoTrackInfo(
        psc: MpvUtil.PlaybackStateCache,
        isHwDecode: Boolean,
        hwdec: String
    ): VideoTrackInfo {
        val isSelectedTrack = isSelected
        return VideoTrackInfo(
            id = id,
            title = title,
            codec = codec ?: "",
            width = if (isSelectedTrack) psc.videoWidth else 0,
            height = if (isSelectedTrack) psc.videoHeight else 0,
            frameRate = if (isSelectedTrack) safeGetDouble("container-fps").toFloat() else 0f,
            isDefault = isDefault,
            isSelected = isSelectedTrack,
            hwDecoder = if (isSelectedTrack && isHwDecode) hwdec else null,
            decoder = if (isSelectedTrack) safeGetString("video-codec") else null,
            decodeMode = if (isSelectedTrack) {
                if (isHwDecode) "硬件解码" else "软件解码"
            } else null,
            pixelFormat = if (isSelectedTrack) safeGetString("video-params/pixelformat") else null,
            bitDepth = if (isSelectedTrack) safeGetLong("video-params/bitdepth").toInt() else 0,
            colorSpace = if (isSelectedTrack) safeGetString("video-params/colormatrix") else null,
            chromaLocation = if (isSelectedTrack) safeGetString("video-params/chroma-location") else null,
            colorPrimaries = if (isSelectedTrack) safeGetString("video-params/primaries") else null,
            colorTransfer = if (isSelectedTrack) safeGetString("video-params/gamma") else null,
            videoRotate = if (isSelectedTrack) psc.videoRotation else 0
        )
    }

    private fun TrackInfo.toAudioTrackInfo(): AudioTrackInfo {
        val isSelectedTrack = isSelected
        return AudioTrackInfo(
            id = id,
            title = title,
            lang = lang,
            codec = codec ?: "",
            sampleRate = if (isSelectedTrack) safeGetLong("audio-params/samplerate").toInt() else 0,
            channelCount = if (isSelectedTrack) safeGetLong("audio-params/channel-count").toInt() else 0,
            isDefault = isDefault,
            isSelected = isSelectedTrack,
            isExternal = isExternal,
            outputFormat = if (isSelectedTrack) safeGetString("audio-out-params/format") else null,
            outputChannels = if (isSelectedTrack) safeGetString("audio-out-params/channels") else null,
            channelLayout = if (isSelectedTrack) safeGetString("audio-params/channels") else null
        )
    }

    private fun TrackInfo.toSubtitleTrackInfo(): SubtitleTrackInfo {
        return SubtitleTrackInfo(
            id = id,
            title = title,
            lang = lang,
            codec = codec ?: "",
            isDefault = isDefault,
            isSelected = isSelected,
            isExternal = isExternal,
            filePath = filePath
        )
    }

    private fun safeGetLong(property: String): Long {
        return try {
            m.getPropertyLong(property) ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    private fun safeGetDouble(property: String): Double {
        return try {
            m.getPropertyDouble(property) ?: 0.0
        } catch (_: Exception) {
            0.0
        }
    }

    private fun safeGetString(property: String): String? {
        return try {
            m.getPropertyString(property)
        } catch (_: Exception) {
            null
        }
    }
}
