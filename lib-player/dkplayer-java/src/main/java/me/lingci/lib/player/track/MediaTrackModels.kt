package me.lingci.lib.player.track

/** Type of media stream represented in a backend-neutral track list. */
enum class MediaTrackType {
    VIDEO,
    AUDIO,
    SUBTITLE,
    UNKNOWN,
}

/** Where a track came from. Backends may use UNKNOWN when the source cannot be identified safely. */
enum class MediaTrackSource {
    EMBEDDED,
    EXTERNAL,
    ADAPTIVE,
    GENERATED,
    UNKNOWN,
}

/**
 * Stable key used by UI/business code to refer back to a backend track.
 *
 * The fields deliberately keep backend/index data opaque: Exo, MPV and IJK all use different id
 * semantics, so only the owning backend should interpret groupIndex/trackIndex/backend.
 */
data class MediaTrackKey(
    val type: MediaTrackType,
    val id: String,
    val groupId: String? = null,
    val groupIndex: Int = -1,
    val trackIndex: Int = -1,
    val backend: String = "",
)

/**
 * Backend-neutral track metadata for display and selection.
 * Missing or backend-specific fields should be left empty rather than leaking Exo/IJK/MPV models.
 */
data class MediaTrack(
    val key: MediaTrackKey,
    val title: String? = null,
    val language: String? = null,
    val codec: String? = null,
    val mimeType: String? = null,
    val bitrate: Long = 0,
    val isSelected: Boolean = false,
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
    val isExternal: Boolean = false,
    val isSupported: Boolean = true,
    val source: MediaTrackSource = MediaTrackSource.UNKNOWN,
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 0f,
    val rotation: Int = 0,
    val sampleRate: Int = 0,
    val channelCount: Int = 0,
    val channelLayout: String? = null,
    val filePath: String? = null,
    val extras: Map<String, String> = emptyMap(),
)

/** Point-in-time track state used by panels and settings screens. */
data class MediaTrackSnapshot(
    val videoTracks: List<MediaTrack> = emptyList(),
    val audioTracks: List<MediaTrack> = emptyList(),
    val subtitleTracks: List<MediaTrack> = emptyList(),
    val disabledTypes: Set<MediaTrackType> = emptySet(),
)
