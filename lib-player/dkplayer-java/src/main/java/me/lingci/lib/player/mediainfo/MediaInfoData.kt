package me.lingci.lib.player.mediainfo

/**
 * Backend-neutral media-info snapshot for UI display only.
 * Empty strings and zero values mean unknown/unavailable, and backend adapters may only be able to
 * populate detailed codec/output fields for currently selected streams.
 */
data class MediaInfoData(
    val containerInfo: ContainerInfo = ContainerInfo(),
    val videoTracks: List<VideoTrackInfo> = emptyList(),
    val audioTracks: List<AudioTrackInfo> = emptyList(),
    val subtitleTracks: List<SubtitleTrackInfo> = emptyList()
)

data class ContainerInfo(
    val fileName: String = "",
    val filePath: String = "",
    val duration: Long = 0,
    val fileSize: Long = 0,
    val containerFormat: String = "",
    val sourceType: String = "",
    val sourceUrl: String = "",
    val playState: String = "",
    val displayMode: String = "",
    val runtimeInfo: String = "",
    val currentPosition: Long = 0,
    val displayRefreshRate: Float = 0f
)

data class VideoTrackInfo(
    val id: Int = -1,
    val title: String? = null,
    val codec: String = "",
    val profile: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 0f,
    val bitRate: Long = 0,
    val isDefault: Boolean = false,
    val isSelected: Boolean = false,
    val hwDecoder: String? = null,
    val decoder: String? = null,
    val decodeMode: String? = null,
    val pixelFormat: String? = null,
    val bitDepth: Int = 0,
    val colorSpace: String? = null,
    val chromaLocation: String? = null,
    val colorPrimaries: String? = null,
    val colorTransfer: String? = null,
    val videoRotate: Int = 0
)

data class AudioTrackInfo(
    val id: Int = -1,
    val title: String? = null,
    val lang: String? = null,
    val codec: String = "",
    val sampleRate: Int = 0,
    val channelCount: Int = 0,
    val bitRate: Long = 0,
    val isDefault: Boolean = false,
    val isSelected: Boolean = false,
    val isExternal: Boolean = false,
    val outputFormat: String? = null,
    val outputChannels: String? = null,
    val channelLayout: String? = null
)

data class SubtitleTrackInfo(
    val id: Int = -1,
    val title: String? = null,
    val lang: String? = null,
    val codec: String = "",
    val isDefault: Boolean = false,
    val isSelected: Boolean = false,
    val isExternal: Boolean = false,
    val filePath: String? = null
)
