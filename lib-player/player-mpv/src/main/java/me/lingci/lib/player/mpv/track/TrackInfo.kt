package me.lingci.lib.player.mpv.track

/**
 * Internal MPV track model. It preserves MPV-native ids until MpvMediaPlayer maps them into the
 * backend-neutral MediaTrack model exposed to app/player-ui code.
 */
data class TrackInfo(
    val id: Int,
    val type: TrackType,
    val title: String?,
    val lang: String?,
    val isDefault: Boolean = false,
    val isSelected: Boolean = false,
    val isExternal: Boolean = false,
    val codec: String? = null,
    val filePath: String? = null
) {
    val displayName: String
        get() = when {
            !title.isNullOrBlank() -> {
                if (!lang.isNullOrBlank() && title != lang) {
                    "$title ($lang)"
                } else {
                    title
                }
            }
            !lang.isNullOrBlank() -> lang
            else -> "${type.displayName} $id"
        }
}

enum class TrackType(val displayName: String) {
    VIDEO("视频"),
    AUDIO("音频"),
    SUBTITLE("字幕");

    companion object {
        fun fromString(value: String): TrackType? {
            return when (value.lowercase()) {
                "video" -> VIDEO
                "audio" -> AUDIO
                "sub" -> SUBTITLE
                else -> null
            }
        }
    }
}

data class TrackList(
    val videoTracks: List<TrackInfo> = emptyList(),
    val audioTracks: List<TrackInfo> = emptyList(),
    val subtitleTracks: List<TrackInfo> = emptyList()
) {
    val hasVideoTracks: Boolean get() = videoTracks.isNotEmpty()
    val hasAudioTracks: Boolean get() = audioTracks.isNotEmpty()
    val hasSubtitleTracks: Boolean get() = subtitleTracks.isNotEmpty()
    
    fun getSelectedVideoTrack(): TrackInfo? = videoTracks.find { it.isSelected }
    fun getSelectedAudioTrack(): TrackInfo? = audioTracks.find { it.isSelected }
    fun getSelectedSubtitleTrack(): TrackInfo? = subtitleTracks.find { it.isSelected }
}
