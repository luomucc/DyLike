package me.lingci.lib.player.exo.mediainfo

import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import me.lingci.lib.player.exo.CustomExoMediaPlayer
import me.lingci.lib.player.mediainfo.AudioTrackInfo
import me.lingci.lib.player.mediainfo.ContainerInfo
import me.lingci.lib.player.mediainfo.MediaInfoData
import me.lingci.lib.player.mediainfo.MediaInfoProvider
import me.lingci.lib.player.mediainfo.SubtitleTrackInfo
import me.lingci.lib.player.mediainfo.VideoTrackInfo

@OptIn(UnstableApi::class)
/** Converts Exo/Media3 track state into generic media-info data for UI panels. */
class ExoMediaInfoProvider(
    private val exoPlayer: CustomExoMediaPlayer
) : MediaInfoProvider {

    override fun getMediaInfo(): MediaInfoData {
        val internalPlayer = exoPlayer.internalPlayer ?: return MediaInfoData()
        val tracks = internalPlayer.currentTracks

        val videoTracks = mutableListOf<VideoTrackInfo>()
        val audioTracks = mutableListOf<AudioTrackInfo>()
        val subtitleTracks = mutableListOf<SubtitleTrackInfo>()

        // IDs here are display fallbacks from Media3 Format, not stable selection keys. Track
        // selection must go through MediaTrackProvider/MediaTrackController instead.
        for (group in tracks.groups) {
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val isSelected = group.isTrackSelected(trackIndex)
                when (group.type) {
                    C.TRACK_TYPE_VIDEO -> {
                        val colorInfo = format.colorInfo
                        videoTracks.add(
                            VideoTrackInfo(
                                id = format.id?.toIntOrNull() ?: trackIndex,
                                title = format.label,
                                codec = format.sampleMimeType ?: format.codecs ?: "",
                                profile = format.codecs,
                                width = format.width,
                                height = format.height,
                                frameRate = format.frameRate,
                                bitRate = format.bitrate.toLong(),
                                isSelected = isSelected,
                                colorSpace = colorInfo?.colorSpace?.let { mapColorSpace(it) },
                                colorTransfer = colorInfo?.colorTransfer?.let { mapColorTransfer(it) },
                                bitDepth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                                    colorInfo?.lumaBitdepth ?: 0
                                } else {
                                    0
                                },
                                videoRotate = format.rotationDegrees
                            )
                        )
                    }
                    C.TRACK_TYPE_AUDIO -> {
                        audioTracks.add(
                            AudioTrackInfo(
                                id = format.id?.toIntOrNull() ?: trackIndex,
                                title = format.label,
                                lang = format.language,
                                codec = format.sampleMimeType ?: format.codecs ?: "",
                                sampleRate = format.sampleRate,
                                channelCount = format.channelCount,
                                bitRate = format.bitrate.toLong(),
                                isSelected = isSelected,
                                channelLayout = mapChannelCount(format.channelCount)
                            )
                        )
                    }
                    C.TRACK_TYPE_TEXT -> {
                        subtitleTracks.add(
                            SubtitleTrackInfo(
                                id = format.id?.toIntOrNull() ?: trackIndex,
                                title = format.label,
                                lang = format.language,
                                codec = format.sampleMimeType ?: "",
                                isSelected = isSelected
                            )
                        )
                    }
                }
            }
        }

        return MediaInfoData(
            containerInfo = ContainerInfo(
                duration = exoPlayer.duration,
                currentPosition = internalPlayer.currentPosition,
                containerFormat = extractContainerFormat(tracks),
                playState = resolvePlayState(internalPlayer),
            ),
            videoTracks = videoTracks,
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks
        )
    }

    override fun isAvailable(): Boolean {
        val player = exoPlayer.internalPlayer ?: return false
        val state = player.playbackState
        return state == Player.STATE_READY
    }

    private fun resolvePlayState(player: androidx.media3.exoplayer.ExoPlayer): String {
        return when (player.playbackState) {
            Player.STATE_BUFFERING -> "缓冲中"
            Player.STATE_READY -> if (player.isPlaying) "播放中" else "暂停"
            Player.STATE_ENDED -> "已结束"
            else -> "就绪"
        }
    }

    private fun extractContainerFormat(tracks: androidx.media3.common.Tracks): String {
        // Exo does not expose one backend-neutral container field here, so use the first video
        // group's container MIME as a display-only heuristic.
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_VIDEO) {
                val format = group.getTrackFormat(0)
                return format.containerMimeType?.substringAfterLast("/") ?: ""
            }
        }
        return ""
    }

    private fun mapColorSpace(colorSpace: Int): String {
        return when (colorSpace) {
            C.COLOR_SPACE_BT601 -> "BT.601"
            C.COLOR_SPACE_BT709 -> "BT.709"
            C.COLOR_SPACE_BT2020 -> "BT.2020"
            else -> "未知"
        }
    }

    private fun mapColorTransfer(colorTransfer: Int): String {
        return when (colorTransfer) {
            C.COLOR_TRANSFER_SDR -> "SDR"
            C.COLOR_TRANSFER_ST2084 -> "PQ (HDR10)"
            C.COLOR_TRANSFER_HLG -> "HLG"
            C.COLOR_TRANSFER_LINEAR -> "Linear"
            C.COLOR_TRANSFER_GAMMA_2_2 -> "Gamma 2.2"
            else -> "未知"
        }
    }

    private fun mapChannelCount(count: Int): String {
        return when (count) {
            1 -> "mono"
            2 -> "stereo"
            6 -> "5.1"
            8 -> "7.1"
            else -> "${count}ch"
        }
    }
}
